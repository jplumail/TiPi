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

package mitiv.deconv;

import java.awt.image.BufferedImage;

import mitiv.invpb.LinearDeconvolver;
import mitiv.linalg.DoubleVector;
import mitiv.linalg.DoubleVectorSpaceWithRank;
import mitiv.linalg.LinearConjugateGradient;
import mitiv.linalg.Vector;
import mitiv.utils.CommonUtils;

/**
 * @author Leger Jonathan
 *
 */
public class Deconvolution{
    /**
     * Compute all the operations with 2D arrays
     */
    public static final int PROCESSING_2D = 0;
    /**
     * Compute all the operations with 1D arrays
     */
    public static final int PROCESSING_1D = 1; 
    /**
     * Compute all the operations with Vectors i.e 1D vector
     */
    public static final int PROCESSING_VECTOR = 2; 
    
    private int standardProcessing = PROCESSING_1D;

    DeconvUtils utils;
    Filter wiener;
    double[][] image;
    double[][] psf;
    double[] image1D;
    double[] psf1D;
    DoubleVector vector_image;
    DoubleVector vector_psf;
    int correction;
    boolean isPsfSplitted = false;
    boolean useVectors;
    //CG needs
    DoubleVectorSpaceWithRank space;
    DoubleVector x;
    DoubleVector w;
    LinearDeconvolver linDeconv;
    int outputValue = LinearConjugateGradient.CONVERGED;

    /**
     * Initial constructor that take the image and the PSF as parameters
     * <br>
     * More options: another correction and use vectors
     * 
     * @param image can be path, bufferedImage or IcyBufferedImage
     * @param PSF can be path, bufferedImage or IcyBufferedImage
     */
    public Deconvolution(Object image, Object PSF){
        this(image,PSF,CommonUtils.SCALE,false);
    }


    /**
     * Initial constructor that take the image and the PSF as parameters
     * <br>
     * More options: another correction and use vectors
     * 
     * @param image can be path, bufferedImage or IcyBufferedImage
     * @param PSF can be path, bufferedImage or IcyBufferedImage
     * @param correction see static {@link CommonUtils}
     */
    public Deconvolution(Object image, Object PSF, int correction){
        this(image,PSF,correction,false);
    }

    /**
     * Initial constructor that take the image and the PSF as parameters
     * <br>
     * More options: another correction and use vectors
     * 
     * @param image can be path, bufferedImage or IcyBufferedImage
     * @param PSF can be path, bufferedImage or IcyBufferedImage
     * @param correction see static {@link CommonUtils}
     * @param useVectors 
     */
    public Deconvolution(Object image, Object PSF, int correction, boolean useVectors){
        utils = new DeconvUtils();
        this.useVectors = useVectors;
        if(image instanceof String){
            if (useVectors) {
                utils.readImageVect((String)image, (String)PSF, false);
            } else {
                utils.readImage((String)image, (String)PSF);
            }
        }else if(image instanceof BufferedImage){
            if (useVectors) {
                utils.readImageVect((BufferedImage)image, (BufferedImage)PSF, false);
            } else {
                utils.readImage((BufferedImage)image, (BufferedImage)PSF);
            }
        }else{
            throw new IllegalArgumentException("Input should be a IcyBufferedImage, BufferedImage or a path");
        }
        this.correction = correction;
        wiener = new Filter();
    }

    /**
     * Simple filter based on the wiener filter.
     * This should be used for the first time
     * <br>
     * Options: job
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolution(double alpha){
        if (useVectors) {
            return firstDeconvolution(alpha, PROCESSING_VECTOR, false);
        } else {
            return firstDeconvolution(alpha, standardProcessing, false);
        }

    }

    /**
     * Simple filter based on the wiener filter.
     * This should be used for the first time
     * <br>
     * Options: job
     * 
     * @param alpha
     * @param isPsfSplitted IF the psf is centered or not
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolution(double alpha , boolean isPsfSplitted){
        if (useVectors) {
            return firstDeconvolution(alpha, PROCESSING_VECTOR, isPsfSplitted);
        } else {
            return firstDeconvolution(alpha, standardProcessing, isPsfSplitted);
        }

    }

    /**
     * Simple filter based on the wiener filter.
     * This should be used for the first time
     * 
     * @param alpha
     * @param job see static PROCESSING_?
     * @param isPsfSplitted isPsfSplitted If the psf is centered or not
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolution(double alpha, int job, boolean isPsfSplitted){
        this.isPsfSplitted = isPsfSplitted;
        switch (job) {
        case PROCESSING_1D:
            return firstDeconvolutionSimple1D(alpha);
        case PROCESSING_2D:
            return firstDeconvolutionSimple(alpha);
        case PROCESSING_VECTOR:
            return firstDeconvolutionVector(alpha);
        default:
            throw new IllegalArgumentException("The job given does not exist");
        }
    }

    /**
     * Simple filter based on the wiener filter.
     * This should be used for the first time.
     * <br>
     * option: job
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    public BufferedImage nextDeconvolution(double alpha){
        if (useVectors) {
            return nextDeconvolution(alpha, PROCESSING_VECTOR);
        } else {
            return nextDeconvolution(alpha, standardProcessing);
        }
    }

    /**
     * Simple filter based on the wiener filter.
     * This should be used for the first time.
     * 
     * @param alpha
     * @param job see static PROCESSING_?
     * @return The bufferedImage for the input value given
     */
    public BufferedImage nextDeconvolution(double alpha, int job){
        switch (job) {
        case PROCESSING_1D:
            return nextDeconvolutionSimple1D(alpha);
        case PROCESSING_2D:
            return nextDeconvolutionSimple(alpha);
        case PROCESSING_VECTOR:
            return nextDeconvolutionVector(alpha);
        default:
            throw new IllegalArgumentException("The job given does not exist");
        }
    }

    /**
     * First deconvolution for the wiener filter
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage firstDeconvolutionSimple(double alpha){
        image = utils.imageToArray(true);
        if (isPsfSplitted) {
            psf = utils.psfToArray(true);
        } else {
            psf = utils.psfPadding(true);
        }
        utils.FFT(image);
        utils.FFT(psf);
        double[][] out = wiener.wiener(alpha, psf, image);
        utils.IFFT(out);
        return(utils.arrayToImage(out, correction));
    }

    /**
     * Will compute less than firstDeconvolution: 1FTT inverse instead
     * of 2FFT + 1 inverse FFT
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage nextDeconvolutionSimple(double alpha){
        double[][] out = wiener.wiener(alpha);
        utils.IFFT(out);
        return(utils.arrayToImage(out, correction));
    }

    /**
     * First deconvolution for the wiener filter
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage firstDeconvolutionSimple1D(double alpha){
        image1D = utils.imageToArray1D(true);
        if (isPsfSplitted) {
            psf1D = utils.psfToArray1D(true);
        } else {
            psf1D = utils.psfPadding1D(true);
        }
        utils.FFT1D(image1D);
        utils.FFT1D(psf1D);
        double[] out = wiener.wiener1D(alpha, psf1D, image1D,utils.width,utils.height);
        utils.IFFT1D(out);
        return(utils.arrayToImage1D(out, correction, true));
    }

    /**
     * Will compute less than firstDeconvolution: 1FTT inverse instead
     * of 2FFT + 1 inverse FFT
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage nextDeconvolutionSimple1D(double alpha){
        double[] out = wiener.wiener1D(alpha);
        utils.IFFT1D(out);
        return(utils.arrayToImage1D(out, correction,true));
    }

    private BufferedImage firstDeconvolutionVector(double alpha){
        vector_image = (DoubleVector) utils.cloneImageVect();
        vector_psf = (DoubleVector) utils.getPsfPadVect();
        //TODO add getPsfVect need change on opening of the image
        utils.FFT1D(vector_image);
        utils.FFT1D(vector_psf);
        Vector out = wiener.wienerVect(alpha, vector_psf, vector_image);
        utils.IFFT1D(out);
        return(utils.arrayToImage(out, correction,true));
    }

    private BufferedImage nextDeconvolutionVector(double alpha){
        Vector out = wiener.wienerVect(alpha);
        utils.IFFT1D(out);
        return(utils.arrayToImage(out, correction,true));
    }

    /**
     * Use the quadratic approximation with circulant approximation
     * This should be used for the first time
     * <br>
     * option: job
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */ 
    public BufferedImage firstDeconvolutionQuad(double alpha){
        if (useVectors) {
            return firstDeconvolutionQuad(alpha, PROCESSING_VECTOR, false);
        } else {
            return firstDeconvolutionQuad(alpha, standardProcessing, false);
        }
    }

    /**
     * @param alpha
     * @param isPsfSplitted If the psf is centered or not
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolutionQuad(double alpha, boolean isPsfSplitted){
        if (useVectors) {
            return firstDeconvolutionQuad(alpha, PROCESSING_VECTOR, isPsfSplitted);
        } else {
            return firstDeconvolutionQuad(alpha, standardProcessing, isPsfSplitted);
        }
    }

    /**
     * Use the quadratic approximation with circulant approximation
     * This should be used for the first time
     * 
     * @param alpha
     * @param job see static PROCESSING_?
     * @param isPsfSplitted isPsfSplitted If the psf is centered or not
     * @return The bufferedImage for the input value given
     */ 
    public BufferedImage firstDeconvolutionQuad(double alpha, int job, boolean isPsfSplitted){
        this .isPsfSplitted = isPsfSplitted;
        switch (job) {
        case PROCESSING_1D:
            return firstDeconvolutionQuad1D(alpha);
        case PROCESSING_2D:
            return firstDeconvolutionQuadSimple(alpha);
        case PROCESSING_VECTOR:
            return firstDeconvolutionQuadVector(alpha);
        default:
            throw new IllegalArgumentException("The job given does not exist");
        }
    }

    /**
     * Use the quadratic approximation with circulant approximation
     * After the initialization use this function
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    public BufferedImage nextDeconvolutionQuad(double alpha){
        if (useVectors) {
            return nextDeconvolutionQuad(alpha, PROCESSING_VECTOR);
        } else {
            return nextDeconvolutionQuad(alpha, standardProcessing);
        }
    }

    /**
     * Use the quadratic approximation with circulant approximation
     * After the initialization use this function
     * 
     * @param alpha
     * @param job see static PROCESSING_?
     * @return The bufferedImage for the input value given
     */
    public BufferedImage nextDeconvolutionQuad(double alpha, int job){
        switch (job) {
        case PROCESSING_1D:
            return nextDeconvolutionQuad1D(alpha);
        case PROCESSING_2D:
            return nextDeconvolutionQuadSimple(alpha);
        case PROCESSING_VECTOR:
            return nextDeconvolutionQuadVector(alpha);
        default:
            throw new IllegalArgumentException("The job given does not exist");
        }
    }

    /**
     * First deconvolution with quadratic option
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage firstDeconvolutionQuadSimple(double alpha){
        image = utils.imageToArray(true);
        if (isPsfSplitted) {
            psf = utils.psfToArray(true);
        } else {
            psf = utils.psfPadding(true);
        }
        utils.FFT(image);
        utils.FFT(psf);
        double[][] out = wiener.wienerQuad(alpha, psf, image);
        utils.IFFT(out);
        return(utils.arrayToImage(out, correction));
    }

    /**
     * Will compute less than firstDeconvolutionQuad: 1FTT inverse instead
     * of 2FFT + 1 inverse FFT
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage nextDeconvolutionQuadSimple(double alpha){
        double[][] out = wiener.wienerQuad(alpha);
        utils.IFFT(out);
        return(utils.arrayToImage(out, correction));
    }

    /**
     * First deconvolution with quadratic option and use in internal only 1D arrays
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage firstDeconvolutionQuad1D(double alpha){
        image1D = utils.imageToArray1D(true);
        if (isPsfSplitted) {
            psf1D = utils.psfToArray1D(true);
        } else {
            psf1D = utils.psfPadding1D(true);
        }
        utils.FFT1D(image1D);
        utils.FFT1D(psf1D);
        double[] out = wiener.wienerQuad1D(alpha, psf1D, image1D, utils.width, utils.height);
        utils.IFFT1D(out);
        return(utils.arrayToImage1D(out, correction,true));
    }

    /**
     * Will compute less than firstDeconvolutionQuad: 1FTT inverse instead
     * of 2FFT + 1 inverse FFT, with 1D arrays optimization
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage nextDeconvolutionQuad1D(double alpha){
        double[] out = wiener.wienerQuad1D(alpha);
        utils.IFFT1D(out);
        return(utils.arrayToImage1D(out, correction, true));
    }

    private BufferedImage firstDeconvolutionQuadVector(double alpha){
        vector_image = (DoubleVector) utils.cloneImageVect();
        vector_psf = (DoubleVector) utils.getPsfPadVect();
        utils.FFT1D(vector_image);
        utils.FFT1D(vector_psf);
        Vector out = wiener.wienerQuadVect(alpha, vector_psf, vector_image);
        utils.IFFT1D(out);
        return(utils.arrayToImage(out, correction,true));
    }

    private BufferedImage nextDeconvolutionQuadVector(double alpha){
        Vector out = wiener.wienerQuadVect(alpha);
        utils.IFFT1D(out);
        return(utils.arrayToImage(out, correction,true));
    }

    private void parseOuputCG(int output){
        //If it does not end normally
        if ( output != LinearConjugateGradient.CONVERGED && output != LinearConjugateGradient.IN_PROGRESS) {
            if (output == LinearConjugateGradient.A_IS_NOT_POSITIVE_DEFINITE) {
                System.err.println("A_IS_NOT_POSITIVE_DEFINITE");
            }else if (output == LinearConjugateGradient.TOO_MANY_ITERATIONS) {
                System.err.println("TOO_MANY_ITERATIONS");
            }else{
                System.err.println("Not ended normally "+output);
            }
        }
    }

    /**
     * Use the conjugate gradients to deconvoluate the image
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolutionCG(double alpha){
        return firstDeconvolutionCG(alpha, PROCESSING_VECTOR, false);
    }

    /**
     * Use the conjugate gradients to deconvoluate the image
     * 
     * @param alpha
     * @param isPsfSplitted If the psf is centered or not
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolutionCG(double alpha, boolean isPsfSplitted){
        return firstDeconvolutionCG(alpha, PROCESSING_VECTOR, isPsfSplitted);
    }

    /**
     * Use the conjugate gradients to deconvoluate the image
     * 
     * @param alpha
     * @param job see static PROCESSING_?
     * @param isPsfSplitted If the psf is centered or not
     * @return The bufferedImage for the input value given
     */
    public BufferedImage firstDeconvolutionCG(double alpha, int job, boolean isPsfSplitted){
        this.isPsfSplitted = isPsfSplitted;
        switch (job) {
        case PROCESSING_VECTOR:
            return firstDeconvolutionCGNormal(alpha);
        default:
            throw new IllegalArgumentException("The job given does not exist");
        }
    }

    /**
     * Use the conjugate gradients to deconvoluate the image.<br>
     * Do less computations and allocations.
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    public BufferedImage nextDeconvolutionCG(double alpha){
        return nextDeconvolutionCG(alpha, PROCESSING_VECTOR);
    }

    /**
     * Use the conjugate gradients to deconvoluate the image.<br>
     * Do less computations and allocations.
     * 
     * @param alpha
     * @param job see static PROCESSING_?
     * @return The bufferedImage for the input value given
     */
    public BufferedImage nextDeconvolutionCG(double alpha, int job){
        switch (job) {
        case PROCESSING_VECTOR:
            return nextDeconvolutionCGNormal(alpha);
        default:
            throw new IllegalArgumentException("The job given does not exist");
        }
    }
    /**
     * Use the conjugate gradients to deconvoluate the image
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage firstDeconvolutionCGNormal(double alpha){
        space = new DoubleVectorSpaceWithRank(utils.width, utils.height);
        if (isPsfSplitted) {
            vector_psf = space.wrap(utils.psfToArray1D(false));
        } else {
            vector_psf = space.wrap(utils.psfPadding1D(false));
        }
        vector_image = space.wrap(utils.imageToArray1D(false));
        /*//We should use this one BUT as there is only CG with vectors, then in the case of no vectors, there is no imageVect
        In others words, if useVectors is False, getImageVect return null.
        if (vector_image == null) {
            vector_image = (DoubleVector) utils.getImageVect();
        }
        if (vector_psf == null) {
            vector_psf = (DoubleVector) utils.getPsfPad();
        }
         */

        x = space.create(0);
        w = space.create(1);

        linDeconv = new LinearDeconvolver(
                space.cloneShape(), vector_image.getData(), vector_psf.getData(), w.getData(), alpha);
        outputValue = linDeconv.solve(x.getData(), 20, false);
        parseOuputCG(outputValue); //print nothing if good, print in err else
        return (utils.arrayToImage1D(x.getData(), correction, false));
    }

    /**
     * Use the conjugate gradients to deconvoluate the image.<br>
     * Do less computations and allocations.
     * 
     * @param alpha
     * @return The bufferedImage for the input value given
     */
    private BufferedImage nextDeconvolutionCGNormal(double alpha){
        boolean verbose = false;
        x = space.create(0);
        linDeconv.setMu(alpha);
        outputValue = linDeconv.solve(x.getData(), 20, false);
        if (verbose) {
            parseOuputCG(outputValue); //print nothing if good, print in err else
        }
        return(utils.arrayToImage1D(x.getData(), correction, false));
    }

    /**
     * @return The value of the result of the computation of the gradients
     */
    public int getOuputValue(){
        return outputValue;
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
