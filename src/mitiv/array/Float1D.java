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

package mitiv.array;

import mitiv.array.impl.FlatFloat1D;
import mitiv.array.impl.StriddenFloat1D;
import mitiv.base.Shape;
import mitiv.base.Shaped;
import mitiv.base.Traits;
import mitiv.base.mapping.FloatFunction;
import mitiv.base.mapping.FloatScanner;
import mitiv.exception.IllegalTypeException;
import mitiv.exception.NonConformableArrayException;
import mitiv.base.indexing.Range;
import mitiv.linalg.shaped.DoubleShapedVector;
import mitiv.linalg.shaped.FloatShapedVector;
import mitiv.linalg.shaped.ShapedVector;
import mitiv.random.FloatGenerator;


/**
 * Define class for comprehensive 1-dimensional arrays of float's.
 *
 * @author Éric Thiébaut.
 */
public abstract class Float1D extends Array1D implements FloatArray {

    protected Float1D(int dim1) {
        super(dim1);
    }

    protected Float1D(int[] dims) {
        super(dims);
    }

    protected Float1D(Shape shape) {
        super(shape);
    }

    @Override
    public final int getType() {
        return type;
    }

    /**
     * Query the value stored at a given position.
     * @param i1 - The index along the 1st dimension.
     * @return The value stored at position {@code (i1)}.
     */
    public abstract float get(int i1);

    /**
     * Set the value at a given position.
     * @param i1    - The index along the 1st dimension.
     * @param value - The value to store at position {@code (i1)}.
     */
    public abstract void set(int i1, float value);

    /*=======================================================================*/
    /* Provide default (non-optimized, except for the loop ordering)
     * implementation of methods that can be coded solely with the "set"
     * and "get" methods. */

    @Override
    public void fill(float value) {
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, value);
        }
    }

    @Override
    public void increment(float value) {
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, get(i1) + value);
        }
    }

    @Override
    public void decrement(float value) {
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, get(i1) - value);
        }
    }

    @Override
    public void scale(float value) {
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, get(i1) * value);
        }
    }

    @Override
    public void map(FloatFunction function) {
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, function.apply(get(i1)));
        }
    }

    @Override
    public void fill(FloatGenerator generator) {
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, generator.nextFloat());
        }
    }

    @Override
    public void scan(FloatScanner scanner)  {
        scanner.initialize(get(0));
        for (int i1 = 1; i1 < dim1; ++i1) {
            scanner.update(get(i1));
        }
    }

    @Override
    public final float[] flatten() {
        return flatten(false);
    }

    @Override
    public float min() {
        float minValue = get(0);
        for (int i1 = 1; i1 < dim1; ++i1) {
            float value = get(i1);
            if (value < minValue) {
                minValue = value;
            }
        }
        return minValue;
    }

    @Override
    public float max() {
        float maxValue = get(0);
        for (int i1 = 1; i1 < dim1; ++i1) {
            float value = get(i1);
            if (value > maxValue) {
                maxValue = value;
            }
        }
        return maxValue;
    }

    @Override
    public float[] getMinAndMax() {
        float[] result = new float[2];
        getMinAndMax(result);
        return result;
    }

    @Override
    public void getMinAndMax(float[] mm) {
        float minValue = get(0);
        float maxValue = minValue;
        for (int i1 = 1; i1 < dim1; ++i1) {
            float value = get(i1);
            if (value < minValue) {
                minValue = value;
            }
            if (value > maxValue) {
                maxValue = value;
            }
        }
        mm[0] = minValue;
        mm[1] = maxValue;
    }

    @Override
    public float sum() {
        float totalValue = get(0);
        for (int i1 = 1; i1 < dim1; ++i1) {
            totalValue += get(i1);;
        }
        return totalValue;
    }

    @Override
    public double average() {
        return (double)sum()/(double)number;
    }

    /**
     * Convert instance into a Byte1D.
     * <p>
     * The operation is lazy, in the sense that {@code this} is returned if it
     * is already of the requested type.
     *
     * @return A Byte1D whose values has been converted into byte's
     *         from those of {@code this}.
     */
    @Override
    public Byte1D toByte() {
        byte[] out = new byte[number];
        int i = -1;
        for (int i1 = 0; i1 < dim1; ++i1) {
            out[++i] = (byte)get(i1);
        }
        return Byte1D.wrap(out, dim1);
    }
    /**
     * Convert instance into a Short1D.
     * <p>
     * The operation is lazy, in the sense that {@code this} is returned if it
     * is already of the requested type.
     *
     * @return A Short1D whose values has been converted into short's
     *         from those of {@code this}.
     */
    @Override
    public Short1D toShort() {
        short[] out = new short[number];
        int i = -1;
        for (int i1 = 0; i1 < dim1; ++i1) {
            out[++i] = (short)get(i1);
        }
        return Short1D.wrap(out, dim1);
    }
    /**
     * Convert instance into an Int1D.
     * <p>
     * The operation is lazy, in the sense that {@code this} is returned if it
     * is already of the requested type.
     *
     * @return An Int1D whose values has been converted into int's
     *         from those of {@code this}.
     */
    @Override
    public Int1D toInt() {
        int[] out = new int[number];
        int i = -1;
        for (int i1 = 0; i1 < dim1; ++i1) {
            out[++i] = (int)get(i1);
        }
        return Int1D.wrap(out, dim1);
    }
    /**
     * Convert instance into a Long1D.
     * <p>
     * The operation is lazy, in the sense that {@code this} is returned if it
     * is already of the requested type.
     *
     * @return A Long1D whose values has been converted into long's
     *         from those of {@code this}.
     */
    @Override
    public Long1D toLong() {
        long[] out = new long[number];
        int i = -1;
        for (int i1 = 0; i1 < dim1; ++i1) {
            out[++i] = (long)get(i1);
        }
        return Long1D.wrap(out, dim1);
    }
    /**
     * Convert instance into a Float1D.
     * <p>
     * The operation is lazy, in the sense that {@code this} is returned if it
     * is already of the requested type.
     *
     * @return A Float1D whose values has been converted into float's
     *         from those of {@code this}.
     */
    @Override
    public Float1D toFloat() {
        return this;
    }
    /**
     * Convert instance into a Double1D.
     * <p>
     * The operation is lazy, in the sense that {@code this} is returned if it
     * is already of the requested type.
     *
     * @return A Double1D whose values has been converted into double's
     *         from those of {@code this}.
     */
    @Override
    public Double1D toDouble() {
        double[] out = new double[number];
        int i = -1;
        for (int i1 = 0; i1 < dim1; ++i1) {
            out[++i] = (double)get(i1);
        }
        return Double1D.wrap(out, dim1);
    }

    @Override
    public Float1D copy() {
        return new FlatFloat1D(flatten(true), shape);
    }

    @Override
    public void assign(ShapedArray arr) {
        if (! getShape().equals(arr.getShape())) {
            throw new NonConformableArrayException("Source and destination must have the same shape.");
        }
        Float1D src;
        if (arr.getType() == Traits.FLOAT) {
            src = (Float1D)arr;
        } else {
            src = (Float1D)arr.toFloat();
        }
        // FIXME: do assignation and conversion at the same time
        for (int i1 = 0; i1 < dim1; ++i1) {
            set(i1, src.get(i1));
        }
    }

    @Override
    public void assign(ShapedVector vec) {
        if (! getShape().equals(vec.getShape())) {
            throw new NonConformableArrayException("Source and destination must have the same shape.");
        }
        // FIXME: much too slow and may be skipped if data are identical (and array is flat)
        if (vec.getType() == Traits.DOUBLE) {
            DoubleShapedVector src = (DoubleShapedVector)vec;
            for (int i1 = 0; i1 < dim1; ++i1) {
                set(i1, (float)src.get(i1));
            }
        } else if (vec.getType() == Traits.FLOAT) {
            FloatShapedVector src = (FloatShapedVector)vec;
            for (int i1 = 0; i1 < dim1; ++i1) {
                set(i1, (float)src.get(i1));
            }
        } else {
            throw new IllegalTypeException();
        }
    }


    /*=======================================================================*/
    /* ARRAY FACTORIES */

    @Override
    public Float1D create() {
        return new FlatFloat1D(getShape());
    }

    /**
     * Create a 1D array of float's with given dimensions.
     * <p>
     * This method creates a 1D array of float's with zero offset, contiguous
     * elements and column-major order.  All dimensions must at least 1.
     * @param dim1 - The 1st dimension of the 1D array.
     * @return A new 1D array of float's.
     * @see {@link Shaped#COLUMN_MAJOR}
     */
    public static Float1D create(int dim1) {
        return new FlatFloat1D(dim1);
    }

    /**
     * Create a 1D array of float's with given shape.
     * <p>
     * This method creates a 1D array of float's with zero offset, contiguous
     * elements and column-major order.
     * @param dims - The list of dimensions of the 1D array (all dimensions
     *               must at least 1).  This argument is not referenced by
     *               the returned object and its contents can be modified
     *               after calling this method.
     * @return A new 1D array of float's.
     * @see {@link Shaped#COLUMN_MAJOR}
     */
    public static Float1D create(int[] dims) {
        return new FlatFloat1D(dims);
    }

    /**
     * Create a 1D array of float's with given shape.
     * <p>
     * This method creates a 1D array of float's with zero offset, contiguous
     * elements and column-major order.
     * @param shape      - The shape of the 1D array.
     * @param cloneShape - If true, the <b>shape</b> argument is duplicated;
     *                     otherwise, the returned object will reference
     *                     <b>shape</b> whose contents <b><i>must not be
     *                     modified</i></b> while the returned object is in
     *                     use.
     * @return A new 1D array of float's.
     * @see {@link Shaped#COLUMN_MAJOR}
     */
    public static Float1D create(Shape shape) {
        return new FlatFloat1D(shape);
    }

    /**
     * Wrap an existing array in a 1D array of float's with given dimensions.
     * <p>
     * The returned 1D array have zero offset, contiguous elements and
     * column-major storage order.  More specifically:
     * <pre>arr.get(i1) = data[i1]</pre>
     * with {@code arr} the returned 1D array.
     * @param data - The data to wrap in the 1D array.
     * @param dim1 - The 1st dimension of the 1D array.
     * @return A 1D array sharing the elements of <b>data</b>.
     * @see {@link Shaped#COLUMN_MAJOR}
     */
    public static Float1D wrap(float[] data, int dim1) {
        return new FlatFloat1D(data, dim1);
    }

    /**
     * Wrap an existing array in a 1D array of float's with given shape.
     * <p>
     * The returned 1D array have zero offset, contiguous elements and
     * column-major storage order.  More specifically:
     * <pre>arr.get(i1) = data[i1]</pre>
     * with {@code arr} the returned 1D array.
     * @param data - The data to wrap in the 1D array.
     * @param dims - The list of dimensions of the 1D array.  This argument is
     *                not referenced by the returned object and its contents
     *                can be modified after the call to this method.
     * @return A new 1D array of float's sharing the elements of <b>data</b>.
     * @see {@link Shaped#COLUMN_MAJOR}
     */
    public static Float1D wrap(float[] data, int[] dims) {
        return new FlatFloat1D(data, dims);
    }

    /**
     * Wrap an existing array in a 1D array of float's with given shape.
     * <p>
     * The returned 1D array have zero offset, contiguous elements and
     * column-major storage order.  More specifically:
     * <pre>arr.get(i1) = data[i1]</pre>
     * with {@code arr} the returned 1D array.
     * @param data       - The data to wrap in the 1D array.
     * @param shape      - The shape of the 1D array.
     * @param cloneShape - If true, the <b>shape</b> argument is duplicated;
     *                     otherwise, the returned object will reference
     *                     <b>shape</b> whose contents <b><i>must not be
     *                     modified</i></b> while the returned object is in
     *                     use.
     * @return A new 1D array of float's sharing the elements of <b>data</b>.
     * @see {@link Shaped#COLUMN_MAJOR}
     */
    public static Float1D wrap(float[] data, Shape shape) {
        return new FlatFloat1D(data, shape);
    }

    /**
     * Wrap an existing array in a 1D array of float's with given dimensions,
     * strides and offset.
     * <p>
     * This creates a 1D array of dimensions {{@code dim1}}
     * sharing (part of) the contents of {@code data} in arbitrary storage
     * order.  More specifically:
     * <pre>arr.get(i1) = data[offset + stride1*i1]</pre>
     * with {@code arr} the returned 1D array.
     * @param data    - The array to wrap in the 1D array.
     * @param offset  - The offset in {@code data} of element (0) of
     *                  the 1D array.
     * @param stride1 - The stride along the 1st dimension.
     * @param dim1    - The 1st dimension of the 1D array.
     * @return A 1D array sharing the elements of <b>data</b>.
     */
    public static Float1D wrap(float[] data,
            int offset, int stride1, int dim1) {
        return new StriddenFloat1D(data, offset, stride1, dim1);
    }

    /**
     * Get a slice of the array.
     *
     * @param idx - The index of the slice along the last dimension of
     *              the array.  The same indexing rules as for
     *              {@link mitiv.base.indexing.Range} apply for negative
     *              index: 0 for the first, 1 for the second, -1 for the
     *              last, -2 for penultimate, <i>etc.</i>
     * @return A FloatScalar view on the given slice of the array.
     */
    public abstract FloatScalar slice(int idx);

    /**
     * Get a slice of the array.
     *
     * @param idx - The index of the slice along the last dimension of
     *              the array.
     * @param dim - The dimension to slice.  For these two arguments,
     *              the same indexing rules as for
     *              {@link mitiv.base.indexing.Range} apply for negative
     *              index: 0 for the first, 1 for the second, -1 for the
     *              last, -2 for penultimate, <i>etc.</i>
     *
     * @return A FloatScalar view on the given slice of the array.
     */
    public abstract FloatScalar slice(int idx, int dim);

    /**
     * Get a view of the array for given range of indices.
     *
     * @param rng1 - The range of indices to select along 1st dimension
     *               (or {@code null} to select all.
     *
     * @return A Float1D view for the given range of the array.
     */
    public abstract Float1D view(Range rng1);

    /**
     * Get a view of the array for given range of indices.
     *
     * @param idx1 - The list of indices to select along 1st dimension
     *               (or {@code null} to select all.
     *
     * @return A Float1D view for the given index selection of the
     *         array.
     */
    public abstract Float1D view(int[] idx1);

    /**
     * Get a view of the array as a 1D array.
     *
     * @return A 1D view of the array.
     */
    @Override
    public abstract Float1D as1D();

}
