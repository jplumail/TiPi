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

package mitiv.optim;

import mitiv.linalg.LinearOperator;
import mitiv.linalg.Vector;
import mitiv.linalg.VectorSpace;

/**
 * Multivariate non-linear optimization by L-BFGS/VMLM method.
 *
 * <p>
 * LBFGS implements a limited memory quasi-Newton method for unconstrained
 * optimization with Broyden-Fletcher-Goldfarb-Shanno (BFGS) updates using
 * Strang's two-loop recursive formula (Nocedal, 1980).
 * </p>
 * 
 * <p>
 * Combined with a Moré & Thuente (1984) line search, the implemented method is
 * similar to VMLM (Nocedal, 1980) or L-BFGS (Liu & Nocedal, 1989) algorithms.
 * </p>
 * 
 * <h3>References</h3>
 *  <ul>
 *  <li>Nocedal, J. "<i>Updating Quasi-Newton Matrices with Limited Storage</i>,"
 *      Mathematics of Computation <b>35</b>, pp.&nbsp;773-782 (1980).</li>
 *  <li>Moré, J. J. & Thuente, D. J. "<i>Line search algorithms with guaranteed
 *      sufficient decrease</i>," TOMS, ACM Press <b>20</b>, pp.&nbsp;286-307
 *      (1994).</li>
 *  <li>Liu, D. C. & Nocedal, J. "<i>On the limited memory BFGS method for
 *      large scale optimization</i>," Mathematical programming <b>45</b>,
 *      pp.&nbsp;503-528 (1989).</li>
 *  </ul>

 * @author Éric Thiébaut.
 *
 */
public class LBFGS extends ReverseCommunicationOptimizerWithLineSearch {

    /** LBFGS approximation of the inverse Hessian */
    protected LBFGSOperator H = null;

    /** Relative threshold for the sufficient descent condition. */
    protected double delta = 0.01;

    /** Small relative size for the initial step or after a restart. */
    protected double epsilon = 1e-3;

    /**
     * Relative threshold for the norm or the gradient (relative to the norm
     * of the initial gradient) for convergence.
     */
    protected double grtol;

    /**
     * Absolute threshold for the norm or the gradient for convergence.
     */
    protected double gatol;

    /** Norm or the initial gradient. */
    protected double ginit;

    /** The norm of the search direction. */
    protected double pnorm;

    /** Lower relative step bound. */
    protected double stpmin = 1e-20;

    /** Upper relative step bound. */
    protected double stpmax = 1e+20;

    /**
     * Attempt to save some memory?
     *
     * <p>
     * To save space, the variable and gradient at the start of a line search
     * may be references to the (s,y) pair of vectors of the LBFGS operator
     * just after the mark.
     * </p>
     */
    private boolean saveMemory = true;

    /** Variables at the start of the line search. */
    protected Vector x0 = null;

    /** Function value at X0. */
    protected  double f0 = 0.0;

    /** Gradient at X0. */
    protected Vector g0 = null;

    /**
     * The (anti-)search direction.
     * 
     * <p>
     * An iterate is computed as: x = x0 - alpha*p with alpha > 0.
     * </p>
     */
    protected Vector p = null;

    /** The current step length. */
    protected double alpha;

    /** Directional derivative at X0. */
    protected  double dg0 = 0.0;

    /** Euclidean norm of the gradient at the last accepted step. */
    protected double gnorm = 0.0;

    /** Euclidean norm of the gradient at X0. */
    protected  double g0norm = 0.0;


    public LBFGS(VectorSpace vsp, int m, LineSearch ls) {
        this(new LBFGSOperator(vsp, m), ls);
    }

    public LBFGS(LinearOperator H0, int m, LineSearch ls) {
        this(new LBFGSOperator(H0, m), ls);
    }

    private LBFGS(LBFGSOperator H, LineSearch ls) {
        this.H = H;
        this.p = H.getOutputSpace().create();
        if (! this.saveMemory) {
            this.x0 = H.getOutputSpace().create();
            this.g0 = H.getInputSpace().create();
        }
        this.lnsrch = ls;
    }

    @Override
    public OptimTask start() {
        evaluations = 0;
        iterations = 0;
        restarts = 0;
        return begin();
    }

    @Override
    public OptimTask restart() {
        ++restarts;
        return begin();
    }

    private OptimTask begin() {
        H.reset();
        return schedule(OptimTask.COMPUTE_FG);
    }

    @Override
    public OptimTask iterate(Vector x, double f, Vector g) {

        switch (task) {

        case COMPUTE_FG:

            /* Caller has computed the function value and the gradient at the
             * current point. */
            ++evaluations;
            if (evaluations > 1) {
                /* A line search is in progress.  Compute directional
                 * derivative and check whether line search has converged. */
                double pg = p.dot(g);
                LineSearchStatus status = lnsrch.iterate(alpha, f, -pg);
                switch (status) {
                case SEARCH:
                    return nextStep(x);
                case CONVERGENCE:
                case WARNING_ROUNDING_ERRORS_PREVENT_PROGRESS:
                    ++iterations;
                    break;
                default:
                    return lineSearchFailure(status);
                }
            }

            /* The current step is acceptable. Check for global convergence. */
            gnorm = g.norm2();
            if (evaluations == 1) {
                ginit = gnorm;
            }
            double gtest = getGradientThreshold();
            return schedule(gnorm <= gtest ? OptimTask.FINAL_X : OptimTask.NEW_X);

        case NEW_X:

            if (evaluations > 1) {
                /* Update the LBFGS matrix. */
                H.update(x, x0, g, g0);
            }

        case FINAL_X:

            /* Compute a search direction, possibly after updating the LBFGS
             * matrix.  We take care of checking whether D = -P is a
             * sufficient descent direction.  As shown by Zoutendijk, this is
             * true if: cos(theta) = -(D/|D|)'.(G/|G|) >= DELTA > 0
             * where G is the gradient. */
            while (true) {
                H.apply(g, p);
                pnorm = p.norm2(); // FIXME: in some cases, can be just GNORM*GAMMA
                double pg = p.dot(g);
                if (pg >= delta*pnorm*gnorm) {
                    /* Accept P (respectively D = -P) as a sufficient ascent
                     * (respectively descent) direction and set the directional
                     * derivative. */
                    dg0 = -pg;
                    break;
                }
                if (H.mp < 1) {
                    /* Initial iteration or recursion has just been
                     * restarted.  This means that the initial inverse
                     * Hessian approximation is not positive definite. */
                    return failure(BAD_PRECONDITIONER);
                }
                /* Restart the LBFGS recursion and loop to use H0 to compute
                 * an initial search direction. */
                H.reset();
                ++restarts;
            }

            /* Save current variables X0, gradient G0 and function value F0. */
            if (saveMemory) {
                /* Use the slot just after the mark to store X0 and G0. */
                x0 = H.s(1);
                g0 = H.y(1);
                H.mp = Math.min(H.mp,  H.m - 1);
            }
            x0.copyFrom(x);
            g0.copyFrom(g);
            g0norm = gnorm;
            f0 = f;

            /* Estimate the length of the first step, start the line search
             * and take the first step along the search direction. */
            if (H.mp >= 1 || H.rule == InverseHessianApproximation.BY_USER) {
                alpha = 1.0;
            } else if (0.0 < epsilon && epsilon < 1.0) {
                double xnorm = x.norm2();
                if (xnorm > 0.0) {
                    alpha = (xnorm/gnorm)*epsilon;
                } else {
                    alpha = 1.0/gnorm;
                }
            } else {
                alpha = 1.0/gnorm;
            }
            LineSearchStatus status = lnsrch.start(f0, dg0, alpha, stpmin*alpha, stpmax*alpha);
            if (status != LineSearchStatus.SEARCH) {
                return lineSearchFailure(status);
            }
            return nextStep(x);

        default:

            /* There must be something wrong. */
            return task;

        }

    }

    /** Build the new step to try as: x = x0 - alpha*p. */
    private OptimTask nextStep(Vector x) {
        alpha = lnsrch.getStep();
        x.axpby(1.0, x0, -alpha, p);
        return schedule(OptimTask.COMPUTE_FG);
    }

    /**
     * Set the absolute tolerance for the convergence criterion.
     * @param gatol - Absolute tolerance for the convergence criterion.
     * @see {@link #setRelativeTolerance}, {@link #getAbsoluteTolerance},
     *      {@link #getGradientThreshold}.
     */
    public void setAbsoluteTolerance(double gatol) {
        this.gatol = gatol;
    }

    /**
     * Set the relative tolerance for the convergence criterion.
     * @param grtol - Relative tolerance for the convergence criterion.
     * @see {@link #setAbsoluteTolerance}, {@link #getRelativeTolerance},
     *      {@link #getGradientThreshold}.
     */
    public void setRelativeTolerance(double grtol) {
        this.grtol = grtol;
    }

    /**
     * Query the absolute tolerance for the convergence criterion.
     * @see {@link #setAbsoluteTolerance}, {@link #getRelativeTolerance},
     *      {@link #getGradientThreshold}.
     */
    public double getAbsoluteTolerance() {
        return gatol;
    }

    /**
     * Query the relative tolerance for the convergence criterion.
     * @see {@link #setRelativeTolerance}, {@link #getAbsoluteTolerance},
     *      {@link #getGradientThreshold}.
     */
    public double getRelativeTolerance() {
        return grtol;
    }

    /**
     * Query the gradient threshold for the convergence criterion.
     * 
     * The convergence of the optimization method is achieved when the
     * Euclidean norm of the gradient at a new iterate is less or equal
     * the threshold:
     * <pre>
     *    max(0.0, gatol, grtol*gtest)
     * </pre>
     * where {@code gtest} is the norm of the initial gradient, {@code gatol}
     * {@code grtol} are the absolute and relative tolerances for the
     * convergence criterion.
     * @return The gradient threshold.
     * @see {@link #setAbsoluteTolerance}, {@link #setRelativeTolerance},
     *      {@link #getAbsoluteTolerance}, {@link #getRelativeTolerance}.
     */
    public double getGradientThreshold() {
        return max(0.0, gatol, grtol*ginit);
    }

    private static final double max(double a1, double a2, double a3) {
        if (a3 >= a2) {
            return (a3 >= a1 ? a3 : a1);
        } else {
            return (a2 >= a1 ? a2 : a1);
        }
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
