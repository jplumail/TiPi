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

import mitiv.base.Traits;
import mitiv.deconv.WeightedConvolutionCost;
import mitiv.linalg.shaped.DoubleShapedVector;
import mitiv.linalg.shaped.DoubleShapedVectorSpace;

/**
 * Abstract class for FFT-based weighted convolution of arrays of double's.
 *
 * @author Éric Thiébaut
 */
public abstract class WeightedConvolutionDouble
     extends WeightedConvolutionCost
{
    /** The data. */
    protected double dat[] = null;

    /** The statistical weights of the data. */
    protected double wgt[] = null;


    /**
     * The following constructors make this class non instantiable, but still
     * let others inherit from this class.
     */
    protected WeightedConvolutionDouble(DoubleShapedVectorSpace objectSpace,
                           DoubleShapedVectorSpace dataSpace) {
        /* Initialize super class and check types. */
        super(objectSpace, dataSpace);
        if (objectSpace.getType() != Traits.DOUBLE) {
            throw new IllegalArgumentException("Object space must be for double data type");
        }
        if (dataSpace.getType() != Traits.DOUBLE) {
            throw new IllegalArgumentException("Data space must be for double data type");
        }
    }

    protected void checkSetup() {
        dat = ((DoubleShapedVector)getData()).getData();
        wgt = ((DoubleShapedVector)getWeights()).getData();
    }

}
