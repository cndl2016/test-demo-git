package com.dm.cn.mcpserver.utils;

import org.springframework.ai.tool.annotation.Tool;

public class MathTools {

	public MathTools() {
	}

	@Tool(description = "加法")
	public int sumNumbers(int number1, int number2) {
		return number1 + number2;
	}

	@Tool(description = "乘法")
	public int multiplyNumbers(int number1, int number2) {
		return number1 * number2;
	}

	@Tool(description = "除法")
	public double divideNumbers(double number1, double number2) {
		return number1 / number2;
	}
}