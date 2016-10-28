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

package mitiv.deconv.impl;

import mitiv.linalg.shaped.ShapedVectorSpace;
import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Implements FFT-based convolution for 1D arrays of double's.
 *
 * @author Éric Thiébaut
 */
public class ConvolutionDouble1D extends ConvolutionDouble {

    /** FFT operator. */
    private DoubleFFT_1D fft = null;

    /** Number of element along 1st dimension of the input variables. */
    private final int dim1;

    /** Offset of output along 1st input dimension. */
    private final int off1;

    /** End of output along 1st input dimension. */
    private final int end1;


    /**
     * Create a new convolution operator for 1D arrays of double's.
     *
     * @param space - The input and output space.
     */
    public ConvolutionDouble1D(ShapedVectorSpace space) {
        super(space);
        if (space.getRank() != 1) {
            throw new IllegalArgumentException("Vector space must be have 1 dimension(s)");
        }
        dim1 = space.getDimension(0);
        off1 = 0;
        end1 = dim1;
    }

    /**
     * Create a new convolution operator for 1D arrays of double's.
     *
     * @param inp
     *        The input space.
     *
     * @param out
     *        The output space.
     *
     * @param off
     *        The position of the output relative to the result of the
     *        convolution.
     */
    public ConvolutionDouble1D(ShapedVectorSpace inp,
                        ShapedVectorSpace out, int[] off) {
        /* Initialize super class and check rank and dimensions (element type
           is checked by the super class constructor). */
        super(inp, out);
        if (inp.getRank() != 1) {
            throw new IllegalArgumentException("Input space is not 1D");
        }
        if (out.getRank() != 1) {
            throw new IllegalArgumentException("Output space is not 1D");
        }
        dim1 = inp.getDimension(0);
        off1 = off[0];
        end1 = off1 + out.getDimension(0);
        if (off1 < 0 || off1 >= dim1) {
            throw new IllegalArgumentException("Out of range offset along 1st dimension");
        }
        if (end1 > dim1) {
            throw new IllegalArgumentException("Data (+ offset) beyond 1st dimension");
        }
    }


    /** Create low-level FFT operator. */
    private final void createFFT() {
        if (fft == null) {
            fft = new DoubleFFT_1D(dim1);
        }
    }

    /** Apply in-place forward complex FFT. */
    @Override
    public final void forwardFFT(double z[]) {
        if (z.length != 2*inpSize) {
            throw new IllegalArgumentException("Bad workspace size");
        }
        timerForFFT.resume();
        if (fft == null) {
            createFFT();
        }
        fft.complexForward(z);
        timerForFFT.stop();
    }

    /** Apply in-place backward complex FFT. */
    @Override
    public final void backwardFFT(double z[]) {
        if (z.length != 2*inpSize) {
            throw new IllegalArgumentException("Bad argument size");
        }
        timerForFFT.resume();
        if (fft == null) {
            createFFT();
        }
        fft.complexInverse(z, false);
        timerForFFT.stop();
    }


    @Override
    public void push(double x[], boolean adjoint) {
        if (x == null || x.length != (adjoint ? outSize : inpSize)) {
            throw new IllegalArgumentException("Bad input size");
        }
        final double zero = 0;
        double z[] = getWorkArray();
        if (adjoint) {
            /* Apply R': set real part of workspace to zero-padded input, set
               the imaginary part to zero. */
            if (outSize == inpSize) {
                /* Output and input have the same size. */
                for (int k = 0, j = 0; j < inpSize; ++j, k += 2) {
                    z[k] = x[j];
                    z[k+1] = zero;
                }
            } else {
                /* Output size is smaller than input size. */
                int j = 0; // index in x array
                int k = 0; // index of real part in z array
                for (int i1 = 0; i1 < off1; ++i1, k += 2) {
                    z[k] = zero;
                    z[k+1] = zero;
                }
                for (int i1 = off1; i1 < end1; ++i1, ++j, k += 2) {
                    z[k] = x[j];
                    z[k+1] = zero;
                }
                for (int i1 = end1; i1 < dim1; ++i1, k += 2) {
                    z[k] = zero;
                    z[k+1] = zero;
                }
            }
        } else {
            /* Apply S */
            for (int j = 0, k = 0; j < inpSize; ++j, k += 2) {
                z[k] = x[j];
                z[k+1] = zero;
            }
        }
    }

    @Override
    public void pull(double x[], boolean adjoint) {
        if (x == null || x.length != (adjoint ? inpSize : outSize)) {
            throw new IllegalArgumentException("Bad input size");
        }
        double z[] = getWorkArray();
        if (adjoint) {
            /* Apply operator S' */
            for (int j = 0, k = 0; j < inpSize; ++j, k += 2) {
                x[j] = z[k];
            }
        } else {
            /* Apply operator R */
            if (outSize == inpSize) {
                /* Output and input have the same size. */
                for (int j = 0, k = 0; j < inpSize; k += 2, ++j) {
                    x[j] = z[k];
                }
            } else {
                /* Output size is smaller than input size. */
                int j = 0; // index in x array
                int k = off1*2;
                for (int i1 = off1; i1 < end1; ++i1, ++j, k += 2) {
                    x[j] = z[k];
                }
            }
        }
    }

}
