import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate

// TensorFlow Lite 모델 파일 경로
val modelPath = "your_model.tflite"

// TensorFlow Lite 모델을 로드
val interpreter: Interpreter

val options = Interpreter.Options()
// GPU 가속을 사용하려면 다음 두 줄을 사용
val gpuDelegate = GpuDelegate()
options.addDelegate(gpuDelegate)
interpreter = Interpreter(loadModelFile(modelPath), options)

// 입력 이미지를 전처리하고 모델에 전달
val inputImage = // 전처리된 이미지
val outputBuffer = Array(1) { FloatArray(NUM_CLASSES) } // NUM_CLASSES는 클래스 수에 따라 조정

interpreter.run(inputImage, outputBuffer)

// 출력 결과 처리
val predictedClass = outputBuffer[0].indexOf(outputBuffer[0].maxOrNull())
// predictedClass는 클래스의 인덱스로, 분류 결과를 나타냄


# 그래들
implementation "androidx.compose.ui:ui:1.0.5"
implementation "androidx.compose.material:material:1.0.5"
implementation "androidx.activity:activity-compose:1.4.0"

# App 코드(Android Studio)
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalDensityAmbient
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TrashClassifierApp() {
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    val imageBitmap = mainViewModel.imageBitmap
    val classificationResult = mainViewModel.classificationResult

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val imageFile = mainViewModel.imageFile
            if (imageFile != null) {
                mainViewModel.classifyImage(context, imageFile)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.Gray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = classificationResult,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Text(
                text = "Take a picture to classify",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(
            onClick = {
                takePictureLauncher.launch(mainViewModel.imageUri)
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Take a Picture")
        }
    }
}

@Composable
fun TrashClassifierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrashClassifierComposeApp(viewModel: MainViewModel) {
    TrashClassifierTheme {
        TrashClassifierApp()
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onFabClick: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val view = LocalView.current
    val dispatcher = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (viewModel.imageFile != null) {
            Image(
                painter = rememberCoilPainter(request = viewModel.imageFile!!.toURI().toURL().toString()),
                contentDescription = "Image"
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BasicTextField(
                        value = viewModel.textField.value,
                        onValueChange = { newValue ->
                            viewModel.textField.value = newValue
                        },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.15.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier
                            .background(Color.White)
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val bottomPadding = (72 * density).dp
                    FloatingActionButton(
                        onClick = {
                            dispatcher.launch {
                                onFabClick()
                            }
                        },
                        backgroundColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = bottomPadding)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_photo_camera_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrashClassifierApp() {
    val viewModel = viewModel<MainViewModel>()
    val context = LocalContext.current
    val result by remember { viewModel.result }

    val onFabClick: () -> Unit = {
        if (viewModel.textField.value.text.isNotBlank()) {
            viewModel.captureImage(context)
        } else {
            showToast(context, "Please enter a label.")
        }
    }

    MainScreen(
        viewModel = viewModel,
        onFabClick = onFabClick
    )

    if (result != null) {
        TrashClassifierResultDialog(result = result!!)
    }
}

@Composable
fun TrashClassifierResultDialog(result: String) {
    AlertDialog(
        onDismissRequest = { /* Handle dialog dismissal */ },
        title = {
            Text("Classification Result")
        },
        text = {
            Text(result)
        },
        confirmButton = {
            Button(
                onClick = { /* Dismiss the dialog */ }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun TrashClassifierTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}

@Composable
fun SplashScreen(onSplashScreenDismiss: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Trash Classifier",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,