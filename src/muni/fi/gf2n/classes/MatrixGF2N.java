/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package muni.fi.gf2n.classes;

import muni.fi.gf2n.interfaces.GaloisFieldMatrixArithmetic;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jakub Lipcak, Masaryk University
 */
public class MatrixGF2N implements GaloisFieldMatrixArithmetic {

    GF2N galoisField;

    public MatrixGF2N(GF2N galoisField) {
        this.galoisField = galoisField;
    }

    public MatrixGF2N(long reducingPolynomial) {
        galoisField = new GF2N(reducingPolynomial);
    }

    @Override
    public long[][] add(long[][] matrix1, long[][] matrix2) {
        isValid(matrix1, matrix2);

        long[][] result = new long[matrix1.length][matrix1[0].length];
        for (int row = 0; row < matrix1.length; row++) {

            for (int col = 0; col < matrix1[0].length; col++) {
                result[row][col] = galoisField.add(matrix1[row][col], matrix2[row][col]);
            }
        }

        return result;
    }

    @Override
    public long[][] subtract(long[][] matrix1, long[][] matrix2) {
        isValid(matrix1, matrix2);
        return add(matrix1, matrix2);
    }

    @Override
    public long[][] transpose(long[][] matrix) {
        isValid(matrix);

        long[][] result = new long[matrix[0].length][matrix.length];
        for (int col = 0; col < matrix.length; col++) {

            for (int row = 0; row < matrix[0].length; row++) {
                result[row][col] = matrix[col][row];
            }
        }
        return result;
    }

    @Override
    public long[][] multiply(long[][] matrix1, long[][] matrix2) {
        long[][] result = new long[matrix1.length][matrix2[0].length];

        if (matrix1.length == 0 || matrix2.length == 0) {
            throw new IllegalArgumentException("Matrix argument is empty, "
                    + "operation cannot be performed.");
        }

        if (matrix1[0].length == 0 || matrix2[0].length == 0) {
            throw new IllegalArgumentException("Argument matrix has empty row, "
                    + "operation cannot be performed.");
        }

        if (matrix1[0].length != matrix2.length) {
            throw new IllegalArgumentException("Argument matrices cannot be multiplied, "
                    + "their dimensions are wrong.");
        }


        for (int x = 0; x < matrix1.length; x++) {
            for (int y = 0; y < matrix2[0].length; y++) {
                long value = 0;
                for (int z = 0; z < matrix1[0].length; z++) {
                    value = galoisField.add(value, galoisField.multiply(matrix1[x][z], matrix2[z][y]));
                }
                result[x][y] = value;
            }
        }

        return result;
    }

    @Override
    public long[][] multiply(long[][] matrix, long scalarValue) {
        isValid(matrix);

        long[][] result = new long[matrix.length][matrix[0].length];
        for (int row = 0; row < matrix.length; row++) {

            for (int col = 0; col < matrix[0].length; col++) {
                result[row][col] = galoisField.multiply(matrix[row][col], scalarValue);
            }
        }

        return result;
    }

    @Override
    public long[][] multiply(long[][] matrix, long[] vector) {
        long matrixVector[][] = new long[vector.length][1];

        for (int x = 0; x < vector.length; x++) {
            matrixVector[x][0] = vector[x];
        }
        return multiply(matrix, matrixVector);
    }

    @Override
    public long[][] inverse(long[][] matrix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long[][] power(long[][] matrix, long exponent) {

        isValid(matrix);

        if (exponent <= 0) {
            throw new IllegalArgumentException("Exponent must be positive number!");
        }

        long[][] result = matrix;
        for (int x = 1; x < exponent; x++) {
            result = multiply(matrix, result);
        }

        return result;
    }

    //Laplace expansion
    @Override
    public long determinant(long[][] matrix) {

        isValid(matrix);

        if (matrix.length != matrix[0].length) {
            throw new IllegalArgumentException("Cannot compute determinant of nonsquare matrix.");
        }

        if (matrix.length == 1) {
            return matrix[0][0];
        }

        long result = 0;

        for (int x = 0; x < matrix.length; x++) {
            //Recursive call is used to compute determinant with Laplace expansion along the first row
            result = galoisField.add(result, galoisField.multiply(matrix[0][x], determinant(subMatrix(matrix, 0, x))));
        }

        return result;

    }

    @Override
    public long rank(long[][] matrix) {

        isValid(matrix);

        long rank = 0;
        long[][] result = gauss(matrix);

        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                if (result[x][y] != 0) {
                    rank++;
                    break;
                }
            }
        }
        return rank;
    }

    @Override
    public long[][] gauss(long[][] matrix) {

        isValid(matrix);

        //prepare result
        long[][] result = new long[matrix.length][matrix[0].length];
        for (int x = 0; x < matrix.length; x++) {
            System.arraycopy(matrix[x], 0, result[x], 0, matrix[0].length);
        }

        for (int diagPos = 1; diagPos < Math.min(matrix.length, matrix[0].length) + 1; diagPos++) {

            long value;
            for (int rowsUnderDiagPos = diagPos; rowsUnderDiagPos < matrix.length; rowsUnderDiagPos++) {

                //find row with pivot at column, that we want to set to zero
                if (result[diagPos - 1][diagPos - 1] == 0) {
                    result = findPivot(result, diagPos - 1, diagPos - 1);
                }

                try {
                    //set value, it will be used to set column at diagPos to zero
                    value = galoisField.divide(result[rowsUnderDiagPos][diagPos - 1], result[diagPos - 1][diagPos - 1]);

                    //subtract from line, pivot will be set to zero and other values will be edited
                    for (int colsUnderDiagPos = diagPos - 1; colsUnderDiagPos < matrix[0].length; colsUnderDiagPos++) {
                        result[rowsUnderDiagPos][colsUnderDiagPos] = galoisField.subtract(galoisField.multiply(result[diagPos - 1][colsUnderDiagPos], value), result[rowsUnderDiagPos][colsUnderDiagPos]);
                    }
                } catch (IllegalArgumentException ex) {
                    //division by zero, column full of zeroes, special case, check it later
                }
            }
        }

        //Result matrix is in row echelon form
        return result;
    }

    //equationMatrix*result = results
    @Override
    public long[] solveLinearEquationsSystem(long[][] equationMatrix, long[] results) {

        isValid(equationMatrix);

        if (equationMatrix.length != equationMatrix[0].length) {
            throw new IllegalArgumentException("Cannot solve linear equations system for nonsquare matrix.");
        }

        if (equationMatrix.length != results.length) {
            throw new IllegalArgumentException("Cannot solve linear equations system: dimension mismatch.");
        }

        if (rank(equationMatrix) != results.length) {
            //SKONTROLOVAT neskor tuto podmienku
            throw new IllegalArgumentException("Cannot solve linear equations system: Some rows in "
                    + "equationMatrix are linearly dependent.");
        }

        //prepare equation matrix
        long[][] eqMat = new long[equationMatrix.length][equationMatrix[0].length + 1];
        for (int x = 0; x < equationMatrix.length; x++) {
            System.arraycopy(equationMatrix[x], 0, eqMat[x], 0, equationMatrix[0].length);
            eqMat[x][equationMatrix[0].length] = results[x];
        }


        //modified Gauss elimination
        for (int diagPos = 1; diagPos < Math.min(eqMat.length, eqMat[0].length) + 1; diagPos++) {

            long value;
            for (int rowsUnderDiagPos = diagPos; rowsUnderDiagPos < eqMat.length; rowsUnderDiagPos++) {

                //find row with pivot at column, that we want to set to zero
                if (eqMat[diagPos - 1][diagPos - 1] == 0) {
                    eqMat = findPivot(eqMat, diagPos - 1, diagPos - 1);
                }

                try {
                    //set value, it will be used to set column at diagPos to zero
                    value = galoisField.divide(eqMat[rowsUnderDiagPos][diagPos - 1], eqMat[diagPos - 1][diagPos - 1]);

                    //subtract from line, pivot will be set to zero and other values will be edited
                    for (int colsUnderDiagPos = diagPos - 1; colsUnderDiagPos < eqMat[0].length; colsUnderDiagPos++) {
                        eqMat[rowsUnderDiagPos][colsUnderDiagPos] = galoisField.subtract(galoisField.multiply(eqMat[diagPos - 1][colsUnderDiagPos], value), eqMat[rowsUnderDiagPos][colsUnderDiagPos]);
                    }
                } catch (IllegalArgumentException ex) {
                    //division by zero, column full of zeroes, special case, check it later
                }
            }
        }

        //set result vector from values prepared in eqMat
        long[] result = new long[equationMatrix[0].length];
        for (int x = eqMat[0].length - 2; x >= 0; x--) {
            for (int y = eqMat[0].length - 2; y >= x; y--) {
                if (y == x) {
                    result[x] = galoisField.divide(eqMat[x][eqMat[0].length - 1], eqMat[x][x]);
                } else {
                    eqMat[x][eqMat[0].length - 1] = galoisField.subtract(eqMat[x][eqMat[0].length - 1],
                            galoisField.multiply(eqMat[x][y], result[y]));
                }
            }
        }

        return result;
    }

    @Override
    public long[] image(long[][] matrix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long[] kernel(long[][] matrix) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compare(long[][] matrix1, long[][] matrix2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void isValid(long[][] matrix1, long[][] matrix2) {

        if (matrix1.length == 0 || matrix2.length == 0) {
            throw new IllegalArgumentException("Matrix argument is empty, "
                    + "operation cannot be performed.");
        }

        if (matrix1[0].length == 0 || matrix2[0].length == 0) {
            throw new IllegalArgumentException("Argument matrix has empty row, "
                    + "operation cannot be performed.");
        }

        if (matrix1[0].length != matrix2[0].length) {
            throw new IllegalArgumentException("Argument matrices have different dimensions, "
                    + "operation cannot be performed.");
        }
    }

    private void isValid(long[][] matrix) {

        if (matrix.length == 0) {
            throw new IllegalArgumentException("Matrix argument is empty, "
                    + "operation cannot be performed.");
        }

        int length = matrix[0].length;
        if (length == 0) {
            throw new IllegalArgumentException("Argument matrix has empty row, "
                    + "operation cannot be performed.");
        }

    }

    //change line row with line with pivot on position column
    private long[][] findPivot(long[][] matrix, int column, int row) {

        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < column + 1; y++) {
                if (matrix[x][y] != 0 && y == column) {
                    return swapLines(matrix, row, x);
                }
                if (matrix[x][y] != 0) {
                    break;
                }
            }
        }

        return matrix;
    }

    private long[][] swapLines(long[][] matrix, int row1, int row2) {

        long[][] result = new long[matrix.length][matrix[0].length];

        for (int x = 0; x < matrix.length; x++) {
            if (x == row1) {
                System.arraycopy(matrix[row2], 0, result[row1], 0, matrix[row2].length);
            }
            if (x == row2) {
                System.arraycopy(matrix[row1], 0, result[row2], 0, matrix[row1].length);
            }
            if (x != row1 && x != row2) {
                System.arraycopy(matrix[x], 0, result[x], 0, matrix[x].length);
            }

        }
        return result;
    }

    //return matrix without row row and column column
    private long[][] subMatrix(long[][] matrix, int row, int column) {

        long[][] result = new long[matrix.length - 1][matrix[0].length - 1];

        int rowIndex = 0;
        int colIndex = 0;

        for (int x = 0; x < matrix.length; x++) {
            colIndex = 0;
            for (int y = 0; y < matrix[0].length; y++) {
                if ((x != row) && (y != column)) {
                    result[rowIndex][colIndex] = matrix[x][y];
                }
                if (column != y) {
                    colIndex++;
                }
            }
            if (row != x) {
                rowIndex++;
            }
        }

        return result;
    }

    public static void printMatrix(long[][] matrix) {

        for (int x = 0; x < matrix.length; x++) {
            System.out.print("[ ");
            for (int y = 0; y < matrix[x].length - 1; y++) {
                System.out.print(matrix[x][y] + ", ");
            }
            System.out.println(matrix[x][matrix[x].length - 1] + " ]");
        }
    }
}
