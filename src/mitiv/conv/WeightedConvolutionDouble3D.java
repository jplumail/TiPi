// *WARNING* This file has been automatically generated by TPP do not edit directly.
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

package mitiv.conv;

import mitiv.array.ShapedArray;
import mitiv.base.Shape;
import mitiv.linalg.Vector;
import mitiv.linalg.shaped.DoubleShapedVector;
import mitiv.linalg.shaped.ShapedVector;

/**
 * Implements a FFT-based weighted convolution for 3D arrays of double's.
 *
 * <p> It is recommended not to directly instantiate this class but rather use
 * one of the factory methods of the parent class
 * {@link mitiv.conv.WeightedConvolutionCost#build}.  Have a look at the
 * documentation of {@link mitiv.conv.WeightedConvolutionCost} for a
 * description of what exactly does this kind of operator.  </p>
 *
 * @author Éric Thiébaut
 *
 * @see mitiv.conv.WeightedConvolutionCost
 */
class WeightedConvolutionDouble3D
extends WeightedConvolutionDouble
{
    /** Number of element along 1st dimension of the work space. */
    private final int dim1;

    /** Offset of data along 1st dimension. */
    private final int off1;

    /** End of data along 1st dimension. */
    private final int end1;

    /** Number of element along 2nd dimension of the work space. */
    private final int dim2;

    /** Offset of data along 2nd dimension. */
    private final int off2;

    /** End of data along 2nd dimension. */
    private final int end2;

    /** Number of element along 3rd dimension of the work space. */
    private final int dim3;

    /** Offset of data along 3rd dimension. */
    private final int off3;

    /** End of data along 3rd dimension. */
    private final int end3;


    /**
     * Create a new FFT-based weighted convolution cost function given
     * a convolution operator.
     *
     * @param cnvl
     *        The convolution operator (the PSF may have not been set).
     */
    public WeightedConvolutionDouble3D(ConvolutionDouble3D cnvl) {
        /* Initialize super class and stor operator. */
        super(cnvl.getInputSpace(), cnvl.getOutputSpace());
        this.cnvl = cnvl;

        /* Store dimensions, offsets, etc. */
        Shape workShape = cnvl.workShape;
        Shape dataShape = cnvl.getOutputSpace().getShape();
        int[] dataOffsets = cnvl.outputOffsets;
        dim1 = workShape.dimension(0);
        off1 = dataOffsets[0];
        end1 = off1 + dataShape.dimension(0);
        dim2 = workShape.dimension(1);
        off2 = dataOffsets[1];
        end2 = off2 + dataShape.dimension(1);
        dim3 = workShape.dimension(2);
        off3 = dataOffsets[2];
        end3 = off3 + dataShape.dimension(2);
    }


    @Override
    protected double _cost(double alpha, Vector x) {
        /* Check whether instance has been fully initialized. */
        checkSetup();

        /* Compute the convolution. */
        cnvl.push((ShapedVector)x, false);
        cnvl.convolve(false);

        /* Integrate cost. */
        double sum = 0.0;
        double z[] = ((ConvolutionDouble) cnvl).getWorkArray();
        int j = 0; // index in data and weight arrays
        int k; // index in work array z
        if (wgt == null) {
            for (int i3 = off3; i3 < end3; ++i3) {
                for (int i2 = off2; i2 < end2; ++i2) {
                    k = 2*(off1 + dim1*(i2 + dim2*i3));
                    for (int i1 = off1; i1 < end1; ++i1) {
                        double r = z[k] - dat[j];
                        sum += r*r;
                        j += 1;
                        k += 2;
                    }
                }
            }
        } else {
            for (int i3 = off3; i3 < end3; ++i3) {
                for (int i2 = off2; i2 < end2; ++i2) {
                    k = 2*(off1 + dim1*(i2 + dim2*i3));
                    for (int i1 = off1; i1 < end1; ++i1) {
                        double w = wgt[j];
                        double r = z[k] - dat[j];
                        sum += w*r*r;
                        j += 1;
                        k += 2;
                    }
                }
            }
        }
        return alpha*sum/2;
    }

    @Override
    protected double _cost(double alpha, Vector x, Vector gx, boolean clr) {
        /* Check whether instance has been fully initialized. */
        checkSetup();

        /* Compute the convolution. */
        cnvl.push((ShapedVector)x, false);
        cnvl.convolve(false);

        /* Integrate cost and gradient. */
        final boolean weighted = (wgt != null);
        final double zero = 0.0;
        final double q = alpha;
        double sum = 0.0;
        double z[] =  ((ConvolutionDouble) cnvl).getWorkArray();
        int j = 0; // index in data and weight arrays
        int k = 0; // index in work array z
        for (int i3 = 0; i3 < off3; ++i3) {
            for (int i2 = 0; i2 < dim2; ++i2) {
                for (int i1 = 0; i1 < dim1; ++i1) {
                    z[k] = zero;
                    z[k+1] = zero;
                    k += 2;
                }
            }
        }
        for (int i3 = off3; i3 < end3; ++i3) {
            for (int i2 = 0; i2 < off2; ++i2) {
                for (int i1 = 0; i1 < dim1; ++i1) {
                    z[k] = zero;
                    z[k+1] = zero;
                    k += 2;
                }
            }
            for (int i2 = off2; i2 < end2; ++i2) {
                for (int i1 = 0; i1 < off1; ++i1) {
                    z[k] = zero;
                    z[k+1] = zero;
                    k += 2;
                }
                if (weighted) {
                    for (int i1 = off1; i1 < end1; ++i1) {
                        double w = wgt[j];
                        double r = z[k] - dat[j];
                        double wr = w*r;
                        sum += r*wr;
                        z[k] = q*wr;
                        z[k+1] = zero;
                        j += 1;
                        k += 2;
                    }
                } else {
                    for (int i1 = off1; i1 < end1; ++i1) {
                        double r = z[k] - dat[j];
                        sum += r*r;
                        z[k] = q*r;
                        z[k+1] = zero;
                        j += 1;
                        k += 2;
                    }
                }
                for (int i1 = end1; i1 < dim1; ++i1) {
                    z[k] = zero;
                    z[k+1] = zero;
                    k += 2;
                }
            }
            for (int i2 = end2; i2 < dim2; ++i2) {
                for (int i1 = 0; i1 < dim1; ++i1) {
                    z[k] = zero;
                    z[k+1] = zero;
                    k += 2;
                }
            }
        }
        for (int i3 = end3; i3 < dim3; ++i3) {
            for (int i2 = 0; i2 < dim2; ++i2) {
                for (int i1 = 0; i1 < dim1; ++i1) {
                    z[k] = zero;
                    z[k+1] = zero;
                    k += 2;
                }
            }
        }

        /* Finalize computation of gradient. */
        double g[] = ((DoubleShapedVector)gx).getData();
        cnvl.convolve(true);
        if (clr) {
            for (j = 0, k = 0; j < g.length; ++j, k += 2) {
                g[j] = z[k];
            }
        } else {
            for (j = 0, k = 0; j < g.length; ++j, k += 2) {
                g[j] += z[k];
            }
        }

        /* Returns cost. */
        return alpha*sum/2;
    }

    @Override
    public void setPSF(ShapedArray psf, int[] off, boolean normalize) {
        cnvl.setPSF(psf, off, normalize);
    }

    @Override
    public void setPSF(ShapedVector psf) {
        cnvl.setPSF(psf);
    }

    @Override
    public ShapedVector getModel(ShapedVector x) {
        /* Compute the convolution. */
        checkSetup();
        ShapedVector dst = cnvl.getOutputSpace().create();
        if (x!=null) {
            cnvl.push(x, false);
            cnvl.convolve(false);
        }
        cnvl.pull(dst, false);
        return dst;
    }
}
