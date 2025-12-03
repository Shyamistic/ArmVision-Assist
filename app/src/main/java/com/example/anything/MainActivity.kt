package com.example.armvisionassist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.armvisionassist.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var cameraControl: CameraControl? = null

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var tts: TextToSpeech? = null

    private var isTorchOn = false
    private var isAutoRead = false
    private var manualTrigger = false

    // Stabilizers
    private var lastSpeechTime = 0L
    private var lastProcessTime = 0L

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.VIBRATE
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) startCamera()
            else Toast.makeText(this, "Permissions needed for AI Vision", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            Toast.makeText(this, "⚠️ Unmute to hear AI results!", Toast.LENGTH_LONG).show()
        }

        tts = TextToSpeech(this, this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) startCamera() else permissionLauncher.launch(requiredPermissions)
        setupControls()
    }

    private fun setupControls() {
        binding.buttonCapture.setOnClickListener {
            performHaptic(50)
            manualTrigger = true
            Toast.makeText(this, "Analyzing...", Toast.LENGTH_SHORT).show()
        }

        binding.switchAutoRead.setOnCheckedChangeListener { _, isChecked ->
            isAutoRead = isChecked
            if (isChecked) {
                lastSpeechTime = 0L // Reset timer so it speaks immediately
                Toast.makeText(this, "HUD Audio: ONLINE", Toast.LENGTH_SHORT).show()
                speak("Systems Online.")
            } else {
                tts?.stop()
                Toast.makeText(this, "HUD Audio: SILENT", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFlashlight.setOnClickListener { toggleTorch(!isTorchOn) }
        binding.sliderZoom.addOnChangeListener { _, value, _ ->
            cameraControl?.setLinearZoom((value - 1.0f) / 4.0f)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processImageProxy(imageProxy) }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e("ArmVision", "Camera init failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            val startTime = System.currentTimeMillis()

            val isPortrait = rotation == 90 || rotation == 270
            val imgWidth = if (isPortrait) mediaImage.height else mediaImage.width
            val imgHeight = if (isPortrait) mediaImage.width else mediaImage.height

            binding.graphicOverlay.post {
                binding.graphicOverlay.setImageSourceInfo(imgWidth, imgHeight, false)
            }

            textRecognizer.process(image)
                .addOnSuccessListener { result ->
                    lastProcessTime = System.currentTimeMillis() - startTime

                    binding.graphicOverlay.clear()
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                binding.graphicOverlay.add(TextGraphic(binding.graphicOverlay, element))
                            }
                        }
                    }

                    runOnUiThread { handleSmartResult(result) }
                }
                .addOnFailureListener { e -> Log.e("ArmVision", "ML Kit failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    // REPLACE your current handleSmartResult with this "Showcase" version
    private fun handleSmartResult(result: Text) {
        val rawText = result.text

        // FEATURE 1: MATRIX IDLE STATE
        if (rawText.isBlank()) {
            val randomHex = (1..8).map { "0123456789ABCDEF".random() }.joinToString("")
            binding.tvStats.text = "SEARCHING // $randomHex..."
            binding.tvStats.setTextColor(android.graphics.Color.parseColor("#88FFFFFF"))
            return
        }

        val analysis = SmartTextProcessor.analyze(result)
        val statsText = StringBuilder()

        // FEATURE 2: DYNAMIC COLOR CODING
        if (analysis.riskLevel > 0) {
            statsText.append("⚠️ CRITICAL THREAT DETECTED | ")
            binding.tvStats.setTextColor(android.graphics.Color.RED) // Turn HUD Red
        } else if (analysis.category.contains("FINANCIAL")) {
            statsText.append("💲 TRANSACTION DETECTED | ")
            binding.tvStats.setTextColor(android.graphics.Color.GREEN) // Turn HUD Green
        } else {
            statsText.append("${analysis.category} | ")
            binding.tvStats.setTextColor(android.graphics.Color.parseColor("#00E5FF")) // Default Cyan
        }

        statsText.append("${lastProcessTime}ms")
        binding.tvStats.text = statsText.toString()

        binding.textRecognized.text = analysis.summary + "\n\n" + rawText

        // FEATURE 3: UI SOUNDS (Run on Action Generation)
        val prevChipCount = binding.chipGroupActions.childCount
        ActionEngine.generateSmartActions(this, rawText, binding.chipGroupActions)
        if (binding.chipGroupActions.childCount > prevChipCount) {
            // Play a system click sound if new buttons appeared
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
        }

        // AUDIO LOGIC (Your existing working logic)
        if (isAutoRead || manualTrigger) {
            val currentTime = System.currentTimeMillis()
            if (analysis.riskLevel > 0 && (currentTime - lastSpeechTime) > 2000) {
                speak("Warning! ${analysis.summary}")
                lastSpeechTime = currentTime
                return
            }
            val isTalking = tts?.isSpeaking ?: false
            if (manualTrigger || ((currentTime - lastSpeechTime) > 3000 && !isTalking)) {
                val toSpeak = "${analysis.summary}. ${analysis.cleanText}"
                if (toSpeak.length > 5 || manualTrigger) {
                    speak(toSpeak)
                    lastSpeechTime = currentTime
                    manualTrigger = false
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ARM_TTS")
    }

    private fun toggleTorch(status: Boolean) {
        isTorchOn = status
        cameraControl?.enableTorch(status)
    }

    private fun performHaptic(duration: Long) {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v.vibrate(duration)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.setLanguage(Locale.US)
    }

    private fun allPermissionsGranted(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        tts?.shutdown()
    }
}