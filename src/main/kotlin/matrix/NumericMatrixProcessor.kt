package matrix

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.text.DecimalFormat
import java.util.*

data class Dimension(val rows: Int, val cols: Int)
private fun Dimension.transpose() = Dimension(cols, rows)
private infix fun Int.by(cols: Int): Dimension = Dimension(this, cols)
private fun Scanner.readDimension(): Dimension {
    val (rows, cols) = this.nextLine().trim().split(' ').map { it.toInt() }
    return rows by cols
}

class Matrix(val dim: Dimension) {
    private val data = Array(dim.rows) { DoubleArray(dim.cols) }
    private val formatter = DecimalFormat("#.##")

    constructor(dim: Dimension, elementInitializer: (Int, Int) -> Double) : this(dim) {
        this.iterate { row, col, _ ->
            this[row][col] = elementInitializer(row, col)
        }
    }

    override fun toString(): String {
        val strings = Array(dim.rows) { row -> Array(dim.cols) { col -> format(this[row][col]) } }
        val colWidths = (0 until dim.cols).map { col ->
            (0 until dim.rows).map { strings[it][col].length }.max()!!
        }
        val sb = StringBuilder()
        iterate({ row, col, _ ->
            sb.append(strings[row][col].padStart(colWidths[col]))
            if (col != dim.cols - 1) {
                sb.append(' ')
            }
        }, { sb.append('\n') })
        return sb.toString()
    }

    private fun format(value: Double): String {
        val normalized = if (value == 0.0) 0.0 else value
        return formatter.format(normalized)
    }

    operator fun get(row: Int) = data[row]
}

private fun Matrix.iterate(eachElementBlock: (Int, Int, Double) -> Unit) {
    iterate(eachElementBlock, { })
}

private fun Matrix.iterate(eachElementBlock: (Int, Int, Double) -> Unit, afterRowBlock: (Int) -> Unit) {
    for (row in 0 until this.dim.rows) {
        for (col in 0 until this.dim.cols) {
            eachElementBlock(row, col, this[row][col])
        }
        afterRowBlock(row)
    }
}

private operator fun Matrix.plus(that: Matrix): Matrix? {
    if (this.dim != that.dim) {
        println("ERROR, dimensions don't match: ${this.dim} vs ${that.dim}")
        return null
    }
    return Matrix(dim) { row, col -> this[row][col] + that[row][col] }
}

private operator fun Double.times(operandMatrix: Matrix): Matrix {
    return Matrix(operandMatrix.dim) { row, col -> this * operandMatrix[row][col] }
}

private operator fun Matrix.times(matrixB: Matrix): Matrix {
    val matrixA = this
    val (aRows, aCols) = matrixA.dim
    val (bRows, bCols) = matrixB.dim

    if (aCols != bRows) {
        throw IllegalArgumentException("matrices A($aRows, $aCols) and B($bRows, $bCols) are not compatible: A columns ($aCols) != B rows ($bRows): \n$matrixA\n\n$matrixB")
    }

    val result = Matrix(aRows by bCols)
    result.iterate { resRow, resCol, _ ->
        for (aColBRow in 0 until aCols) {
            result[resRow][resCol] += matrixA[resRow][aColBRow] * matrixB[aColBRow][resCol]
        }
    }
    return result
}

private operator fun Matrix.div(divider: Double) = Matrix(dim) { row, col -> this[row][col] / divider }

private fun Matrix.transposeMain(): Matrix {
    return Matrix(dim.transpose()) { row, col -> this[col][row] }
}

private fun Matrix.transposeSecondary(): Matrix {
    return Matrix(dim.transpose()) { row, col -> this[dim.cols - col - 1][dim.rows - row - 1] }
}

private fun Matrix.transposeVertical(): Matrix {
    return Matrix(dim) { row, col -> this[row][dim.cols - col - 1]}
}

private fun Matrix.transposeHorizontal(): Matrix {
    return Matrix(dim) { row, col -> this[dim.rows - row - 1][col] }
}

private fun Matrix.minor(targetRow: Int, targetCol: Int): Double {
    val source = this
    return Matrix(source.dim.rows - 1 by source.dim.cols - 1) { minorRow, minorCol ->
        val sourceRow = if (minorRow < targetRow) minorRow else minorRow + 1
        val sourceCol = if (minorCol < targetCol) minorCol else minorCol + 1
        source[sourceRow][sourceCol]
    }.determinant()
}

private fun Matrix.cofactor(targetRow: Int, targetCol: Int): Double {
    val sign = if ((targetRow + targetCol) % 2 == 0) 1 else -1
    return sign * this.minor(targetRow, targetCol)
}

private fun Matrix.determinant(): Double {
    if (dim.rows != dim.cols) throw IllegalArgumentException("Non-square matrices don't have determinants: $dim\n$this")
    if (dim.rows < 1) throw IllegalStateException("Determinant can't be computed for dimension: $dim\n$this")
    if (dim.rows == 1) return this[0][0]
    if (dim.rows == 2) return this[0][0] * this[1][1] - this[0][1] * this[1][0]

    val referenceRow = 0
    var determinant = 0.0
    for (referenceCol in 0 until this.dim.cols) {
        determinant += this[referenceRow][referenceCol] * cofactor(referenceRow, referenceCol)
    }
    return determinant
}

private fun Matrix.adjoint() = Matrix(this.dim) { row, col -> this.cofactor(row, col) }.transposeMain()

private fun Matrix.inverse(): Matrix? {
    val determinant = this.determinant()
    if (determinant == 0.0) return null
    return this.adjoint() / determinant
}

private fun Scanner.readMatrixData(dimension: Dimension): Matrix {
    val matrix = Matrix(dimension)

    for (r in 0 until dimension.rows) {
        val tokens = nextLine().trim().split(' ')
        if (tokens.size != dimension.cols) {
            throw InputMismatchException("Expected row[$r] size is ${dimension.cols} but was ${tokens.size}: $tokens, ")
        }
        for (c in 0 until dimension.cols) {
            matrix[r][c] = tokens[c].toDouble()
        }
    }

    return matrix
}

private fun Scanner.readMatrix(matrixName: String = ""): Matrix {
    print("Enter size of ${matrixName}matrix: ")
    val dimension = this.readDimension()
    println("Enter the ${matrixName}matrix:")
    return this.readMatrixData(dimension)
}

private fun Scanner.readScalar() = this.readDoubleTillEol()
private fun Scanner.readIntTillEol() = this.nextLine().toInt()
private fun Scanner.readDoubleTillEol() = this.nextLine().toDouble()

fun main() {
    val scanner = Scanner(System.`in`).useLocale(Locale.US)
    mainLoop@while (true) {
        print("""
            1. Add matrices
            2. Multiply matrix to a constant
            3. Multiply matrices
            4. Transpose matrices
            5. Calculate a determinant
            6. Inverse matrix
            0. Exit
            Your choice: 
        """.trimIndent())
        when (scanner.readIntTillEol()) {
            1 -> {
                val matrixA = scanner.readMatrix("first ")
                val matrixB = scanner.readMatrix("second ")
                println("The sum result is:")
                println(matrixA + matrixB)
            }
            2 -> {
                val matrix = scanner.readMatrix()
                print("Enter the scalar: ")
                val scalar = scanner.readScalar()
                println("The scalar product result is:")
                println(scalar * matrix)
            }
            3 -> {
                val matrixA = scanner.readMatrix("first ")
                val matrixB = scanner.readMatrix("second ")
                println("The dot product result is:")
                println(matrixA * matrixB)
            }
            4 -> {
                print("""
                    1. Main diagonal
                    2. Side diagonal
                    3. Vertical line
                    4. Horizontal line
                    Your choice: 
                """.trimIndent())
                val transposeOption = scanner.readIntTillEol()
                val matrix = scanner.readMatrix()
                println("The result is:")
                println(when (transposeOption) {
                    1 -> matrix.transposeMain()
                    2 -> matrix.transposeSecondary()
                    3 -> matrix.transposeVertical()
                    4 -> matrix.transposeHorizontal()
                    else -> "Invalid transpose option, try again"
                })
            }
            5 -> {
                val matrix = scanner.readMatrix()
                println("The determinant is:")
                println(matrix.determinant())
            }
            6 -> {
                val matrix = scanner.readMatrix()
                when (val inverse = matrix.inverse()) {
                    null -> println("Matrix has no inverse (determinant = 0)")
                    else -> println(inverse)
                }
            }
            0 -> break@mainLoop
        }
    }
}

