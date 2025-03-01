package com.example.commapps

import com.example.commapps.ui.theme.CommAppsTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Stack

const val allowedOpers = "+-*/^"
const val allowedSymbols = "+-*/^."
class NothingToDoException(message: String) : Exception(message)

@Composable
fun Calculator(colors: ColorScheme) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(colors.background), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center ) {
        OutlinedTextField(
            value = input,
            maxLines = 4,
            label = { Text("Enter the expression") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            onValueChange = { newValue ->
                input = newValue.filter { it.isDigit() || it in allowedSymbols }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.background,
                unfocusedContainerColor = colors.background,
                focusedIndicatorColor = colors.inversePrimary,
                focusedLabelColor = colors.inversePrimary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        ElevatedButton(colors = ButtonDefaults.buttonColors(containerColor = colors.inversePrimary), onClick = {
            result = try {
                formatResult(evaluate(input))
            } catch (e: NothingToDoException) {
                e.message ?: "Nothing to do"
            } catch (e: Exception) {
                if (e.message == "long overflow"){
                    "Too much to calc"
                }
                else{
                    "Error"
                }
            }
        }) {
            Text("Calculate", fontSize = 20.sp, color = colors.primary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(color = colors.primary, text = result, fontSize = 32.sp)
    }
}

fun formatResult(value: Long): String = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

fun evaluate(expr: String): Long {

    if (!expr.any {it in allowedOpers}) {
        throw NothingToDoException("Nothing to do")
    }
    val output = mutableListOf<String>()
    val operators = Stack<Char>()
    var i = 0
    while (i < expr.length) {
        val c = expr[i]
        if (c.isDigit() || c == '.') {
            var num = ""
            while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                num += expr[i]
                i++
            }
            output.add(num)
            continue
        }
        if (c == '(') operators.push(c)
        else if (c == ')') {
            while (operators.peek() != '(') output.add(operators.pop().toString())
            operators.pop()
        } else if (isOperator(c)) {
            while (operators.isNotEmpty() && precedence(operators.peek()) >= precedence(c)) output.add(operators.pop().toString())
            operators.push(c)
        }
        i++
    }
    while (operators.isNotEmpty()) output.add(operators.pop().toString())
    val stack = Stack<Long>()
    for (token in output) {
        val num = token.toLongOrNull()
        if (num != null) stack.push(num)
        else if (token.length == 1 && isOperator(token[0])) {
            val b = stack.pop()
            val a = stack.pop()
            stack.push(applyOp(a, b, token[0]))
        }
    }
    return stack.pop()
}

fun isOperator(c: Char) = c in allowedOpers

fun precedence(c: Char) = when(c) {
    '+', '-' -> 1
    '*', '/' -> 2
    '^' -> 3
    else -> -1
}

fun applyOp(a: Long, b: Long, op: Char): Long = when(op) {
    '+' -> a + b
    '-' -> a - b
    '*' -> a * b
    '/' -> a / b
    '^' -> a.pow(b)
    else -> 0L
}

private fun Long.pow(exp: Long): Long {
    if (exp < 0) throw IllegalArgumentException("Отрицательная степень не поддерживается")
    if (this <= 0L) throw IllegalArgumentException("Основание должно быть положительным, чтобы избежать отрицательного или нулевого результата")

    var result = 1L
    var base = this
    var exponent = exp

    while (exponent > 0) {
        if (exponent and 1L == 1L) {
            result = Math.multiplyExact(result, base)
        }
        exponent = exponent shr 1
        if (exponent > 0) {
            base = Math.multiplyExact(base, base)
        }
    }
    return result
}



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CommAppsTheme(dynamicColor = false) {
                val color = MaterialTheme.colorScheme
                Calculator(color)
            }
        }
    }
}