package uj.wmii.pwj.spreadsheet;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Spreadsheet {
    private HashMap<String, Double> knownResults = new HashMap<>();
    private HashMap<String, Double> overwrittenCells = new HashMap<>();

    private String extractFormulaElem(String formula, Pattern regexp) {
        Matcher matcher = regexp.matcher(formula);
        if (matcher.find()) {
            int offsetOneBeyondMatch = matcher.end();
            int offsetStartMatch = matcher.start();
            return matcher.group().substring(1, offsetOneBeyondMatch - 1 - offsetStartMatch);
        }
        else {
            throw new IllegalArgumentException("Could not extract formula '" + formula + "' elements");
        }
    }

    public String[] parseFormulaicCell(String cellToParse) {
        // returns 3-array of [OPERATION, LHS, RHS]
        String operation = extractFormulaElem(cellToParse, Pattern.compile("=.+\\("));
        String leftHandOperand = extractFormulaElem(cellToParse, Pattern.compile("\\(.+,"));
        String rightHandOperand = extractFormulaElem(cellToParse, Pattern.compile(",.+\\)"));

        return new String[]{operation, leftHandOperand, rightHandOperand};
    }

    public Integer[] parseReference(String referenceElem) {
        // supports <= 26 columns
        int asciiCapitalAPosition = 65;

        String referenceProper = referenceElem.substring(1);
        Integer columnIndex = (int)referenceProper.charAt(0) - asciiCapitalAPosition;
        Integer rowIndex = Integer.parseInt(referenceProper.substring(1)) - 1;

        return new Integer[]{rowIndex, columnIndex};
    }

    public Double calculateCellElement(String elem, String[][] inputSheet) {
        if (!elem.contains("$")) {
            return Double.parseDouble(elem);
        }
        else {
            Integer[] rowAndCol = parseReference(elem);
            Integer row = rowAndCol[0];
            Integer col = rowAndCol[1];

            String rowColHash = String.valueOf(row) + "," + String.valueOf(col);
            if (overwrittenCells.containsKey(rowColHash))
                return overwrittenCells.get(rowColHash);

            String recurseNextElem = inputSheet[row][col];
            Double result = calculateCell(recurseNextElem, inputSheet);
            overwrittenCells.put(rowColHash, result);

            return result;
        }
    }

    public Double performFormulaCalculation(String operation, Double left, Double right) {
        switch (operation) {
            case "ADD": return left + right;
            case "SUB": return left - right;
            case "MUL": return left * right;
            case "DIV": return left / right;
            case "MOD":
                if (right < 0) right = -right;
                if (left < 0) left = right - left;
                int temp = (int)left.doubleValue() % (int)right.doubleValue();
                return (double)temp;
            default:
                return 0.0;
        }
    }

    public Double calculateCell(String cellToCalculate, String[][] inputSheet) {
        if (knownResults.containsKey(cellToCalculate))
            return knownResults.get(cellToCalculate);

        boolean cellContainsFormula = cellToCalculate.contains("=");
        boolean cellContainsReference = cellToCalculate.contains("$");

        Double finalResult;
        if (cellContainsFormula) {
            String[] formulaParts = parseFormulaicCell(cellToCalculate);
            Double leftResult = calculateCellElement(formulaParts[1], inputSheet);
            Double rightResult = calculateCellElement(formulaParts[2], inputSheet);
            finalResult = performFormulaCalculation(formulaParts[0], leftResult, rightResult);
        }
        else if (cellContainsReference){
            finalResult = calculateCellElement(cellToCalculate, inputSheet);
        }
        else {
            finalResult = Double.parseDouble(cellToCalculate);
        }

        knownResults.put(cellToCalculate, finalResult);
        return finalResult;
    }

    public String[][] calculate(String[][] input) {
        // iterate over row, col: for each cell, calculate using recursion; hash results

        String[][] out = new String[input.length][input[0].length];
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                String cell = input[i][j];
                if (cell.equals("$A1"))
                    System.out.print("sth");
                Double cellResult = calculateCell(cell, input);
                out[i][j] = String.valueOf(Math.round(Math.floor(cellResult)));
            }
        }

        return out;
    }
}
