package com.ejemplo.calculadorawebservice

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var etNumero1: EditText
    private lateinit var etNumero2: EditText
    private lateinit var tvResultado: TextView
    private lateinit var btnSumar: Button
    private lateinit var btnRestar: Button
    private lateinit var btnMultiplicar: Button
    private lateinit var btnDividir: Button
    private lateinit var btnLimpiar: Button

    private val TAG = "CalculadoraWS"

    // URL del servicio web público (funciona 100%)
    private val BASE_URL = "http://www.dneonline.com/calculator.asmx"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_calculadora)  // <- NUEVO NOMBRE

        inicializarControles()
        configurarListeners()

        // Mostrar mensaje de uso
        Toast.makeText(this, "Usando servicio web público", Toast.LENGTH_LONG).show()
        tvResultado.text = "Servicio público listo"
    }

    private fun inicializarControles() {
        etNumero1 = findViewById(R.id.etNumero1)
        etNumero2 = findViewById(R.id.etNumero2)
        tvResultado = findViewById(R.id.tvResultado)
        btnSumar = findViewById(R.id.btnSumar)
        btnRestar = findViewById(R.id.btnRestar)
        btnMultiplicar = findViewById(R.id.btnMultiplicar)
        btnDividir = findViewById(R.id.btnDividir)
        btnLimpiar = findViewById(R.id.btnLimpiar)
    }

    private fun configurarListeners() {
        btnSumar.setOnClickListener { ejecutarOperacion("Add") }
        btnRestar.setOnClickListener { ejecutarOperacion("Subtract") }
        btnMultiplicar.setOnClickListener { ejecutarOperacion("Multiply") }
        btnDividir.setOnClickListener { ejecutarOperacion("Divide") }
        btnLimpiar.setOnClickListener {
            limpiarCampos()
        }
    }

    private fun ejecutarOperacion(operacion: String) {
        val num1 = etNumero1.text.toString()
        val num2 = etNumero2.text.toString()

        // Validar campos
        if (num1.isEmpty() || num2.isEmpty()) {
            Toast.makeText(this, "Ingrese ambos números", Toast.LENGTH_SHORT).show()
            etNumero1.requestFocus()
            return
        }

        // Validar división por cero
        if (operacion == "Divide" && num2 == "0") {
            Toast.makeText(this, "No se puede dividir por cero", Toast.LENGTH_SHORT).show()
            etNumero2.requestFocus()
            return
        }

        // Ejecutar operación
        OperacionAsyncTask().execute(operacion, num1, num2)
    }

    private fun limpiarCampos() {
        etNumero1.text.clear()
        etNumero2.text.clear()
        tvResultado.text = "0"
        etNumero1.requestFocus()
        Toast.makeText(this, "Campos limpiados", Toast.LENGTH_SHORT).show()
    }

    // ============================================
    // ASYNCTASK PARA CONSUMO DEL WEB SERVICE
    // ============================================
    private inner class OperacionAsyncTask : AsyncTask<String, Void, String>() {

        override fun onPreExecute() {
            tvResultado.text = "Procesando..."
            Toast.makeText(this@MainActivity, "Consultando servicio web...", Toast.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg params: String): String {
            val operacion = params[0]
            val num1 = params[1]
            val num2 = params[2]

            Log.d(TAG, "Ejecutando $operacion con $num1 y $num2")

            var connection: HttpURLConnection? = null
            try {
                // Crear conexión
                val url = URL(BASE_URL)
                connection = url.openConnection() as HttpURLConnection

                // Configurar conexión
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
                connection.setRequestProperty("SOAPAction", "http://tempuri.org/$operacion")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.doInput = true

                // Crear XML SOAP (formato específico para este servicio)
                val soapRequest = crearSOAPRequest(operacion, num1, num2)

                Log.d(TAG, "SOAP Request: ${soapRequest.take(200)}...")

                // Enviar request
                val outputStream = connection.outputStream
                val writer = DataOutputStream(outputStream)
                writer.writeBytes(soapRequest)
                writer.flush()
                writer.close()

                // Obtener respuesta
                val responseCode = connection.responseCode
                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Leer respuesta exitosa
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    Log.d(TAG, "SOAP Response: ${response.toString().take(200)}...")

                    // Extraer resultado
                    return extraerResultadoSOAP(response.toString(), operacion)

                } else {
                    // Leer error
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        val errorReader = BufferedReader(InputStreamReader(errorStream))
                        val errorResponse = StringBuilder()
                        var errorLine: String?
                        while (errorReader.readLine().also { errorLine = it } != null) {
                            errorResponse.append(errorLine)
                        }
                        errorReader.close()
                        return "Error $responseCode: ${errorResponse.toString().take(100)}"
                    }
                    return "Error HTTP: $responseCode"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error en conexión: ${e.message}", e)
                return "Error de conexión: ${e.message}"
            } finally {
                connection?.disconnect()
            }
        }

        override fun onPostExecute(result: String) {
            // Mostrar resultado
            tvResultado.text = result

            // Mostrar notificación
            if (result.contains("Error")) {
                Toast.makeText(this@MainActivity, "Error: $result", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Operación fallida: $result")
            } else {
                Toast.makeText(this@MainActivity, "¡Operación exitosa!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Resultado: $result")
            }
        }

        // ============================================
        // CREAR REQUEST SOAP (formato específico)
        // ============================================
        private fun crearSOAPRequest(operacion: String, num1: String, num2: String): String {
            return """<?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                               xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                               xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <${operacion} xmlns="http://tempuri.org/">
                      <intA>$num1</intA>
                      <intB>$num2</intB>
                    </${operacion}>
                  </soap:Body>
                </soap:Envelope>"""
        }

        // ============================================
        // EXTRAER RESULTADO DEL XML SOAP
        // ============================================
        private fun extraerResultadoSOAP(xml: String, operacion: String): String {
            return try {
                // Buscar etiqueta específica de resultado
                val resultTag = "<${operacion}Result>"
                if (xml.contains(resultTag)) {
                    val start = xml.indexOf(resultTag) + resultTag.length
                    val end = xml.indexOf("</${operacion}Result>")
                    val resultado = xml.substring(start, end).trim()

                    // Formatear resultado
                    when (operacion) {
                        "Divide" -> String.format("%.2f", resultado.toDouble())
                        else -> resultado
                    }
                } else {
                    // Buscar alternativas
                    when {
                        xml.contains(":int>") -> {
                            val start = xml.indexOf(":int>") + 5
                            val end = xml.indexOf("</", start)
                            xml.substring(start, end).trim()
                        }
                        xml.contains(":double>") -> {
                            val start = xml.indexOf(":double>") + 8
                            val end = xml.indexOf("</", start)
                            String.format("%.2f", xml.substring(start, end).trim().toDouble())
                        }
                        else -> {
                            Log.d(TAG, "XML no reconocido, mostrando parte: ${xml.take(300)}")
                            "Formato no reconocido"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parseando XML: ${e.message}")
                "Error parseando respuesta"
            }
        }
    }
}