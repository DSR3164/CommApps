package com.example.commapps

import com.example.commapps.ui.theme.CommAppsTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

const val allowedOpers = "+-*/^"
const val allowedSymbols = "$allowedOpers.()"

@Composable
fun Calculator(colors: ColorScheme) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = input,
            maxLines = 4,
            textStyle = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight(600),
                fontFamily = FontFamily.Monospace
            ),
            label = { Text("Enter the expression") },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { newValue ->
                input = newValue.filter { it.isDigit() || it in allowedSymbols }
                result = try {
                    evaluate(input).toString()
                } catch (e: Exception) {
                    when (e.message) {
                        "long overflow" -> "Too much to calc"
                        "Nothing to do" -> ""
                        else -> "Error"

                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.background,
                unfocusedContainerColor = colors.background,
                focusedIndicatorColor = colors.inversePrimary,
                focusedLabelColor = colors.inversePrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = result,
            color = colors.primary,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .animateContentSize(animationSpec = tween(300))
        )
    }
}

fun evaluate(expr: String): Double {

    if (!expr.any { it in allowedOpers }) {
        throw Exception("Nothing to do")
    }
    val output = mutableListOf<String>()
    val operators = ArrayDeque<Char>()
    var i = 0
    while (i < expr.length) {
        val c = expr[i]
        if (c.isDigit() || c == '.' ||
            (c == '-' && (i == 0 || expr[i - 1] in "$allowedOpers(") && (i + 1 < expr.length && (expr[i + 1].isDigit() || expr[i + 1] == '.')))) {
            val numBuilder = StringBuilder()
            if (c == '-') {
                numBuilder.append(c)
                i++
            }
            while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                numBuilder.append(expr[i])
                i++
            }
            output.add(numBuilder.toString())
            continue
        }
        when (c) {
            '(' -> operators.addLast(c)
            ')' -> {
                while (operators.isNotEmpty() && operators.last() != '(') {
                    output.add(operators.removeLast().toString())
                }
                if (operators.isNotEmpty() && operators.last() == '(') {
                    operators.removeLast()
                } else {
                    throw Exception("Mismatched parentheses")
                }
            }
            else -> if (isOperator(c)) {
                while (operators.isNotEmpty() &&
                    if (c == '^') precedence(operators.last()) > precedence(c)
                    else precedence(operators.last()) >= precedence(c)
                ) {
                    output.add(operators.removeLast().toString())
                }
                operators.addLast(c)
            }
        }
        i++
    }
    while (operators.isNotEmpty()) {
        val op = operators.removeLast()
        if (op == '(' || op == ')') throw Exception("Mismatched parentheses")
        output.add(op.toString())
    }

    val stack = ArrayDeque<Double>()
    for (token in output) {
        token.toDoubleOrNull()?.let { stack.addLast(it) } ?: run {
            if (token.length == 1 && isOperator(token[0])) {
                if (stack.size < 2) throw Exception("Invalid Expression")
                val b = stack.removeLast()
                val a = stack.removeLast()
                stack.addLast(applyOp(a, b, token[0]))
            }
        }
    }
    return stack.last()
}

fun isOperator(c: Char) = c in allowedOpers

fun precedence(c: Char) = when (c) {
    '+', '-' -> 1
    '*', '/' -> 2
    '^' -> 3
    else -> -1
}

fun applyOp(a: Double, b: Double, op: Char): Double = when (op) {
    '+' -> a + b
    '-' -> a - b
    '*' -> a * b
    '/' -> a / b
    '^' -> a.pow(b)
    else -> throw IllegalArgumentException("Unsupported operator")
}



class CalculatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CommAppsTheme(dynamicColor = false) {
                Calculator(MaterialTheme.colorScheme)
            }
        }
    }
}