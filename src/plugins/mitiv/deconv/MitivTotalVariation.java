/*
 * This file is part of TiPi (a Toolkit for Inverse Problems and Imaging)
 * developed by the MitiV project.
 *
 * Copyright (c) 2014 the MiTiV project, http://mitiv.univ-lyon1.fr/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package plugins.mitiv.deconv;

import java.util.ArrayList;

import mitiv.array.Double2D;
import mitiv.array.Double3D;
import mitiv.array.DoubleArray;
import mitiv.invpb.ReconstructionJob;
import mitiv.invpb.ReconstructionViewer;
import mitiv.utils.CommonUtils;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent;
import icy.sequence.SequenceListener;
import commands.TotalVariationDeconvolution;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;

public class MitivTotalVariation extends EzPlug implements SequenceListener {

    public class tvViewer implements ReconstructionViewer{

        @Override
        public void display(ReconstructionJob job) {
            setResult();
        }
    }

    TotalVariationDeconvolution tvDec;

    private Sequence sequence;

    private double mu = 0.1;
    private double epsilon = 0.1;
    private double grtol = 0.1;
    private double gatol = 0.0;
    private int maxIter = 50;
    private boolean goodInput = true;
    private boolean psfSplitted = false;
    private boolean computeNew = true;
    
    private int width = -1;
    private int height = -1;
    private int sizeZ = -1;

    private EzVarSequence psfSequence = new EzVarSequence("PSF");
    private EzVarSequence imageSequence = new EzVarSequence("Image");
    private EzVarBoolean eZpsfSplitted = new EzVarBoolean("Is the psf splitted ?", psfSplitted);
    private EzVarDouble eZmu = new EzVarDouble("Mu", 0, Double.MAX_VALUE, 0.1);
    private EzVarDouble eZepsilon = new EzVarDouble("Epsilon", 0, Double.MAX_VALUE, 1);
    private EzVarDouble eZgrtol = new EzVarDouble("grtol", 0, 1, 0.1);
    private EzVarInteger eZmaxIter = new EzVarInteger("Max Iterations", -1, Integer.MAX_VALUE, 1);

    IcyBufferedImage img;
    IcyBufferedImage psf;

    private void message(String info){
        new AnnounceFrame(info);
        goodInput = false;
    }

    @Override
    protected void initialize() {

        eZmu.setValue(mu);
        eZepsilon.setValue(epsilon);
        eZgrtol.setValue(grtol);
        eZmaxIter.setValue(maxIter);

        addEzComponent(psfSequence);
        addEzComponent(imageSequence);
        addEzComponent(eZpsfSplitted);
        addEzComponent(eZmu);
        addEzComponent(eZepsilon);
        addEzComponent(eZgrtol);
        addEzComponent(eZmaxIter);

    }

    @Override
    protected void execute() {
        //Getting all values
        goodInput = true;
        mu = eZmu.getValue();
        epsilon = eZepsilon.getValue();
        grtol = eZgrtol.getValue();
        maxIter = eZmaxIter.getValue();
        psfSplitted = eZpsfSplitted.getValue();
        //Testing epsilon and grtol
        if (mu < 0) {
            message("Regularization level MU must be strictly positive");
        }
        if (epsilon <= 0) {
            message("Threshold level EPSILON must be strictly positive");
        }
        if (grtol == 0 || grtol == 1) {
            message("grtol canno't be 0 or 1");
        }

        //Test if we have the image and the psf ...
        if(imageSequence.getValue() == null || psfSequence.getValue() == null){
            //If there is a missing parameter we notify the user with the missing parameter as information
            String message = "You have forgotten to give ";
            String messageEnd = "";
            if (imageSequence.getValue() == null) {
                messageEnd = messageEnd.concat("the image ");
            }
            if(psfSequence.getValue() == null) {
                if (imageSequence.getValue() == null) {
                    messageEnd = messageEnd.concat("and ");
                }
                messageEnd = messageEnd.concat("a PSF");
            }
            message(message+messageEnd);
        }else{
            //And if the sizes are matching
            img = imageSequence.getValue().getFirstNonNullImage();
            psf = psfSequence.getValue().getFirstNonNullImage();
            //if the user they the psf is splitted and the psf and image are not of the same size
            if (psfSplitted && (img.getWidth() != psf.getWidth() || img.getHeight() != psf.getHeight())) {
                message("The image and the psf should be of same size");
            }
            //if the user make a mistake between psf and image
            if (psf.getWidth() > img.getWidth() || psf.getHeight() > img.getHeight()) {
                message("The psf canno't be larger than the image");
            }
        }
        if (goodInput) {
            //Launching computation
            tvDec = new TotalVariationDeconvolution();
            tvDec.setRegularizationWeight(mu);
            tvDec.setRegularizationThreshold(epsilon);
            tvDec.setRelativeTolerance(grtol);
            tvDec.setAbsoluteTolerance(gatol);
            tvDec.setMaximumIterations(maxIter);
            tvDec.setViewer(new tvViewer());

            // Read the blurred image and the PSF.
            width = img.getWidth();
            height = img.getHeight();
            sizeZ = imageSequence.getValue().getSizeZ();

            ArrayList<IcyBufferedImage> listImg = imageSequence.getValue().getAllImage();
            ArrayList<IcyBufferedImage> listPSf= psfSequence.getValue().getAllImage();
            //3D
            DoubleArray imgArray, psfArray;
            int[] shape;
            if (sizeZ == 1) {
                shape = new int[]{width, height};
                imgArray =  Double2D.wrap(CommonUtils.imageToArray1D(img, false), shape);
                psfArray =  Double2D.wrap(CommonUtils.psfPadding1D(img, psf, false), shape);
            } else {
                shape = new int[]{width, height, sizeZ};
                imgArray =  Double3D.wrap(CommonUtils.icyImage3DToArray1D(listImg, width, height, sizeZ, false), shape);
                psfArray =  Double3D.wrap(CommonUtils.shiftIcyPsf3DToArray1D(listPSf, width, height, sizeZ, false), shape);
            }
            tvDec.setData(imgArray);
            tvDec.setPsf(psfArray);

            //Compution HERE
            tvDec.deconvolve();
            //Getting the results
            setResult();
            computeNew = true;
        }
    }

    private void setResult(){
        if (sequence == null || computeNew == true) {
            sequence = new Sequence();
            sequence.addListener(this);
            addSequence(sequence);
            computeNew = false;
        }
        double[] in = tvDec.getResult().flatten();
        for (int j = 0; j < sizeZ; j++) {
            double[] temp = new double[width*height];
            for (int i = 0; i < width*height; i++) {
                temp[i] = in[i+j*width*height];
            }
            sequence.setImage(0,j, new IcyBufferedImage(width, height, temp));
        }
        sequence.setName("TV mu:"+mu);
    }

    @Override
    public void clean() {
        if (sequence != null) {
            sequence.close();
        }
        if (tvDec != null) {
            tvDec.stop();
        }

    }

    @Override
    public void sequenceChanged(SequenceEvent sequenceEvent) {
    }

    @Override
    public void sequenceClosed(Sequence seq) {
        computeNew = true;
    }


}




/*
 * Local Variables:
 * mode: Java
 * tab-width: 8
 * indent-tabs-mode: nil
 * c-basic-offset: 4
 * fill-column: 78
 * coding: utf-8
 * ispell-local-dictionary: "american"
 * End:
 */