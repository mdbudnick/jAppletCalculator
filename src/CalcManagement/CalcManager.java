package CalcManagement;

import java.util.ArrayList;
import java.util.Arrays;

import DisplayManagement.DisplayManager;

public class CalcManager {
	private DisplayManager dispManager;
	private HistoricalCalculations histCalcs;
	private int histInd;
	private Double[][] workset;
	private boolean firstOperation, firstCalc;	// Flags to initialize workset
	private boolean newOperation, firstOperand; // Flags for operation step
	String currentDisplay;
	private ArrayList<String> histDisp;
	
	public CalcManager(DisplayManager dispManager, HistoricalCalculations histCalcs) {
		this.dispManager = dispManager;
		this.histCalcs = histCalcs;
		this.histDisp = this.histCalcs.getHistDisp();
		resetCalcManager();
	}
	
	//Command to call when histCalcs or currentDisplay is ever changed
	private void updateDisplays() {
		dispManager.updateDisplays(currentDisplay, histDisp);
	}
	
	//Buttons that change display
	public void button(String string) {
		if (getDisplayingResult() || (getCleared() && string != "0")) {
			firstEntry(string);
		} else {
			notFirstEntry(string);
		}
		
		updateDisplays();
	}
	
	private void firstEntry(String string) {
		if (currentDisplay == "NaN") { // Force clear after squaring negative
			System.out.println("firstentry NaN");
			return;
		} else if (string == ".") {
			currentDisplay = "0.";
			this.dispManager.setDecimalPoint(true);
			this.dispManager.setDisplayingResult(false);
		} else {
			if (this.getDecimalPoint()) {
				currentDisplay = currentDisplay + string;
			} else {
				currentDisplay = string;
			}
			this.dispManager.setCleared(false);
			this.dispManager.setDisplayingResult(false);
		}
	}
	
	private void notFirstEntry(String string) {
		if (string == ".") {
			if (!getDecimalPoint()) { // Do nothing if decimal point already there.
				currentDisplay = currentDisplay + string;
				this.dispManager.setDecimalPoint(true);
			}
		} else {
			if (!getDisplayingResult()) {
				currentDisplay = currentDisplay + string;
			} else {
				currentDisplay = string;
			}
		}		
	}
	
	public void button(int operator) {
		// Current Calculation = workset[1];
		
		// Workset array key: { Operator Code, First Operand, Second Operand, Result, Modified(bool 1/0) };
		// Operator Code: {0: +, 1: -, 2: *, 3: /, 4: sqrt, 5: =}
		
		if (getCleared() || currentDisplay == "NaN") { // Do nothing
			return;
		} else if (firstOperation || firstCalc) { // Check if we have just started
			// If first calculation we need to create the workset
			initialOp(operator);
		} else if (newOperation) {
			System.out.println("new operation");
			workset[1][0] = (double) operator;
			workset[1][1] = Double.parseDouble(currentDisplay);
			newOperation = false;
			firstOperand = true;
			pushWorkset(workset[1]);
		} else if (getDisplayingResult() && firstOperand) { // Change operator
			System.out.println("changing operator");
			workset[1][0] = (double) operator;
			pushWorkset(workset[1]);
		} else if (!getDisplayingResult() && firstOperand) {
			System.out.println("adding second operand");
			workset[1][2] = Double.parseDouble(currentDisplay);
			processCurrentCalc(operator);
		}
				
		this.dispManager.setDecimalPoint(false);
		this.dispManager.setDisplayingResult(true);
		updateDisplays();
	}
	
	public void squareRoot() {
		if (getCleared() || currentDisplay == "NaN") { // Do nothing
			return;
		}
			if (Double.parseDouble(currentDisplay) < 0) { // Break calculator if squaring negative number
				currentDisplay = "NaN";
				updateDisplays();
				return;
			}
			workset[1][0] = 4.0;
			workset[1][1] = workset[0][3];
			workset[1][2] = workset[0][3];
			processCurrentCalc(4);
			if (Double.parseDouble(currentDisplay) < 0) { // Break calculator if squaring negative number
				currentDisplay = "NaN";
				updateDisplays();
				return;
			}
			workset[1][0] = 4.0;
			workset[1][1] = workset[0][3];
			workset[1][2] = workset[0][3];
			processCurrentCalc(4);
		double prevOp = workset[0][0];
		// Check for special prevOps sqrt(4) and equals(5)
		if (prevOp >= 4) {
			// Check if current and prevop were sqrt => nest operations
			if (prevOp % 4 == 0) {
				if (Double.parseDouble(currentDisplay) < 0) { // Break calculator if squaring negative number
					currentDisplay = "NaN";
					updateDisplays();
					return;
				}
				decrementHistInd();
				exchangePrevCurrent();
				String squares = "4" + workset[1][0];
				workset[1][0] = Double.parseDouble(squares);
				workset[1][4] = 1.0;
				processCurrentCalc(4);
			}
		}
	}

	public void equals() {
		if (getCleared() || currentDisplay == "NaN") { // Do nothing
			return;
		} else if (!firstOperation && firstCalc) {
			initialOp(5);
			this.dispManager.setDecimalPoint(false);
			this.dispManager.setDisplayingResult(true);
			updateDisplays();
		} else if (!getDisplayingResult() && firstOperand) {
			System.out.println("adding second operand");
			workset[1][2] = Double.parseDouble(currentDisplay);
			processCurrentCalc(5);
			this.dispManager.setDecimalPoint(false);
			this.dispManager.setDisplayingResult(true);
			updateDisplays();
		}
		
	}

	private void initialOp(int operator) {
		if (!firstOperation && firstCalc) {
			workset[1][2] = Double.parseDouble(currentDisplay);
			processCurrentCalc(operator);
		} else {
			Double firstOp = (double) operator;
			workset[1] = new Double[]{ firstOp, Double.parseDouble(currentDisplay), null, null, 1.0 };
			pushWorkset(workset[1]);
			firstOperation = false;
			if (operator == 4) {
				workset[1][2] = Double.parseDouble(currentDisplay);
				processCurrentCalc(operator);
			}
		}
	}

	private void processCurrentCalc(int operator) {
		if (firstOperation) {
			firstOperation = false;
		}
		if (firstCalc) {
			firstCalc = false;
		}
		
		Double result = executeCurrentOp();
		currentDisplay = "" + result; // Change display to result
		workset[1][3] = result; // Put in result
		workset[1][4] = 1.0; // Row is modified
		workset[0] = Arrays.copyOf(workset[1], 5);
		pushWorkset(workset[0]);
		incrementHistInd();
		workset[1] = new Double[]{null, null, null, null, 1.0}; // New row, modified (1.0) from nothing.
		newOperation = true;
		firstOperand = false;
		if (operator != 5) {
			workset[1][0] = (double) operator;
			workset[1][1] = workset[0][3];
			newOperation = false;
			firstOperand = true;
			System.out.println("processed and added first operand");
		}
		
		pushWorkset(workset[1]);
	}

	private void pushWorkset(Double[] workset) {
		histCalcs.append(workset, histInd);
	}

	private Double executeCurrentOp() {
		Double result = null;
		int operator = workset[1][0].intValue();
		
		// Remove extra 4s if nested sqrt
		while (operator > 10) {
			operator = operator % 10;
		}
		
		System.out.println("operator: " + operator);
		// Operator Code: {0: +, 1: -, 2: *, 3: /, 4: sqrt, 5: =}
		switch(operator) {
			case(0):
				System.out.println("0");
				result = workset[1][1] + workset[1][2];
				break;
			case(1):
				System.out.println("1");
				result = workset[1][1] - workset[1][2];
				break;
			case(2):
				System.out.println("2");
				result = workset[1][1] * workset[1][2];
				break;
			case(3):
				System.out.println("3");
				result = workset[1][1] / workset[1][2];
				break;
			case(4):
				//Hold the root for the nested string
				System.out.println("4");
				result = calcNestSqrtDbl(workset[1][0].intValue(), workset[1][2]); // workset[1][2] being the root number of the sqrts are nested
				break;
			case(5):
				System.out.println("5");
				result = Double.parseDouble(currentDisplay);
				break;
			default:
			
		}
		System.out.println("result: " + result);
		return result;
	}
	
	private void exchangePrevCurrent() {
		workset[1] = Arrays.copyOf(workset[0], 5);
		System.out.println("prev operators!!! " + workset[1][0]);
	}

	private Object[] getHistory(int ind) {
		return this.histCalcs.getHistory(ind);
	}

	private void incrementHistInd() {
		this.histCalcs.incrementHistInd();
	}
	
	private void decrementHistInd() {
		this.histCalcs.decrementHistInd();
	}

	public void clear() {
		System.out.println("clear");
		resetCalcManager();
		clearHistory();
		clearDisplay();
		updateDisplays();
	}

	private void resetCalcManager() {
		currentDisplay = "0";
		firstOperation = true;
		firstCalc = true;
		newOperation = true;
		firstOperand = false;
		workset = new Double[2][];
		this.histInd = histCalcs.getHistInd();
		updateDisplays();
	}

	private void clearDisplay() {
		this.dispManager.clearDisplay();
	}

	private void clearHistory() {
		this.histCalcs.clearHistory();
	}

	public void backspace() {		
		int length = currentDisplay.length();
		if (length > 0 && !getDisplayingResult()) {
			StringBuilder sb = new StringBuilder(currentDisplay);
			sb.deleteCharAt(length-1);
			if (sb.length() == 0) {
				sb.append("0");
			}
			currentDisplay = sb.toString();
			updateDisplays();
		}
	}

	public void negate() {
		if (!getCleared()) {
			Double negatedVal = 0 - Double.parseDouble(currentDisplay);
			currentDisplay = "" + negatedVal;
			if (currentCalcIncomplete() && getDisplayingResult()) { // If displaying the first operand
				workset[1][1] = negatedVal;
				if (workset[1][0] == 5) {
					workset[1][4] = negatedVal; // Change the total for an = operations
				}
			}
			updateDisplays();
		}
		
	}

	private boolean currentCalcIncomplete() {
		return workset[1][2] == null;
	}

	private boolean getHistCleared() {
		return this.histCalcs.getHistCleared();
	}

	private Double calcNestSqrtDbl(int operators, Double root) {
		int numSqrt = ("" + operators).length();
		Double value = root;
		for (int i = 0; i < numSqrt; i++) {
			value = Math.sqrt(value);
		}
		return value;
	}
	
	private boolean getDisplayingResult() {
		return this.dispManager.getDisplayingResult();
	}

	private boolean getCleared() {
		return this.dispManager.getCleared();
	}
	
	private boolean getDecimalPoint() {
		return this.dispManager.getDecimalPoint();
	}

}
