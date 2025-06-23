package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ActivityProfileBinding
import com.example.myapplication.model.Usuario
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import de.hdodenhof.circleimageview.CircleImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.myapplication.model.SupabaseStorageService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class ProfileFragment : Fragment() {
    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!
    private var db = Firebase.firestore
    private var selectedImageUri1: Uri? = null
    private lateinit var usuario: Usuario
    private lateinit var imagen1: ImageView
    private lateinit var imagen2: ImageView
    private lateinit var imagen3: ImageView
    private lateinit var email: String
    private var timer: Timer? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://ptfvjswhcnwcyoossfrt.supabase.co")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(SupabaseStorageService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagen1 = binding.imv1
        imagen2 = binding.imv2
        imagen3 = binding.imv3
        binding.buttonAddFriend.visibility = View.GONE
        binding.edadysexo.visibility = View.GONE

        // Set up delete button click listeners
        binding.btnDelete1.setOnClickListener {
            deleteImage(1)
        }
        binding.btnDelete2.setOnClickListener {
            deleteImage(2)
        }
        binding.btnDelete3.setOnClickListener {
            deleteImage(3)
        }

        // Obtener el email del usuario actual
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            email = currentUser.email ?: ""
            Log.d("EMAIL",email)
            loadUserProfile(email)
            startPeriodicUpdates()
        }

        binding.button3.setOnClickListener {
            val intent = Intent(requireContext(), AjustesPerfil::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", usuario.nombre_usuario)
            startActivity(intent)
        }

        imagen1.setOnClickListener {
            openImagePicker1()
        }
        imagen2.setOnClickListener {
            openImagePicker2()
        }
        imagen3.setOnClickListener {
            openImagePicker3()
        }
    }

    private fun loadUserProfile(email: String) {
        db.collection("usuarios")
            .whereEqualTo("e_mail", email)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val user = document.toObject(Usuario::class.java)
                    if (user != null) {
                        usuario = user
                        binding.profileName.text = user.nombre_usuario

                        Glide.with(this)
                            .load(user.imagen_perfil.toUri())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.profileImage2)

                        Glide.with(this)
                            .load(user.imagen1.toUri())
                            .into(binding.imv1)

                        Glide.with(this)
                            .load(user.imagen2.toUri())
                            .into(binding.imv2)

                        Glide.with(this)
                            .load(user.imagen3.toUri())
                            .into(binding.imv3)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.d("Error", e.toString())
            }
    }

    private fun openImagePicker1() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage1.launch(intent)
    }

    private fun openImagePicker2() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage2.launch(intent)
    }

    private fun openImagePicker3() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage3.launch(intent)
    }

    fun formatTimestamp(timestamp: Timestamp): String {
        val date = Date(timestamp.seconds * 1000) // Convertir segundos a milisegundos
        val sdf = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault())
        return sdf.format(date)
    }

    private val pickImage1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri1 = uri
            Glide.with(this)
                .load(uri)
                .into(binding.imv1)

            // ✅ Mueve aquí la lógica de subida
            usuario?.let { user ->
                uploadImageToSupabase(imagen1, uri, requireContext(), user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen1" to downloadUrl)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", user.e_mail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                document.reference.update(update)
                            }
                        }
                }
            }
        }
    }

    private val pickImage2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri1 = uri
            Glide.with(this)
                .load(uri)
                .into(binding.imv2)

            // ✅ Mueve aquí la lógica de subida
            usuario?.let { user ->
                uploadImageToSupabase(imagen2, uri, requireContext(), user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen2" to downloadUrl)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", user.e_mail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                document.reference.update(update)
                            }
                        }
                }
            }
        }
    }

    private val pickImage3 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            selectedImageUri1 = uri
            Glide.with(this)
                .load(uri)
                .into(binding.imv3)
            // ✅ Mueve aquí la lógica de subida
            usuario?.let { user ->
                uploadImageToSupabase(imagen3, uri, requireContext(), user.nombre_usuario.toString()) { downloadUrl ->
                    val update = mapOf("imagen3" to downloadUrl)
                    db.collection("usuarios")
                        .whereEqualTo("e_mail", user.e_mail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot) {
                                document.reference.update(update)
                            }
                        }
                }
            }
        }
    }

    fun uploadImageToSupabase(imageView: ImageView, imageUri: Uri, context: Context, username: String, onSuccess: (String) -> Unit) {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(imageUri) ?: return
        val fileBytes = inputStream.readBytes()
        val requestBody = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        var imagen = 0

        if(imageView == imagen1){
            imagen = 1
        }else if(imageView == imagen2){
            imagen = 2
        }else if(imageView == imagen3){
            imagen = 3
        }


        val filePart = MultipartBody.Part.createFormData(
            "file",
            "perfil.jpg",
            requestBody
        )

        val authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0ZnZqc3doY253Y3lvb3NzZnJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc0MDg4MDQsImV4cCI6MjA2Mjk4NDgwNH0.8K2P087GLNqcndYB_1FJW2tples19SUGb9t_5GByigk"
        val bucket = "images"
        val formattedDate = formatTimestamp(Timestamp.now())
        val fileName = "$username/imagen${imagen}_${formattedDate}.jpg"// 🗂️ imagen única por usuario

        val deleteCall = service.deleteImage(authHeader, bucket, fileName)
        deleteCall.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("Supabase", "Imagen previa eliminada (o no existía): ${response.code()}")

                // Paso 2: Subir la nueva imagen
                val uploadCall = service.uploadImage(authHeader, bucket, fileName, filePart)
                uploadCall.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("Supabase", "Imagen subida con éxito")
                            val publicUrl =
                                "https://ptfvjswhcnwcyoossfrt.supabase.co/storage/v1/object/public/$bucket/$fileName"
                            onSuccess(publicUrl)
                        } else {
                            Log.e(
                                "Supabase",
                                "Error al subir imagen: ${response.code()} - ${
                                    response.errorBody()?.string()
                                }"
                            )
                            Toast.makeText(
                                context,
                                "Error al subir imagen a Supabase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("Supabase", "Fallo al subir imagen", t)
                        Toast.makeText(
                            context,
                            "Fallo al subir imagen a Supabase",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.w("Supabase", "No se pudo eliminar la imagen previa (puede no existir)", t)

                // Aun si falla, intentamos subir la imagen
                val uploadCall = service.uploadImage(authHeader, bucket, fileName, filePart)
                uploadCall.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("Supabase", "Imagen subida con éxito")
                            val publicUrl =
                                "https://ptfvjswhcnwcyoossfrt.supabase.co/storage/v1/object/public/$bucket/$fileName"
                            onSuccess(publicUrl)
                        } else {
                            Log.e(
                                "Supabase",
                                "Error al subir imagen: ${response.code()} - ${
                                    response.errorBody()?.string()
                                }"
                            )
                            Toast.makeText(
                                context,
                                "Error al subir imagen a Supabase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("Supabase", "Fallo al subir imagen", t)
                        Toast.makeText(
                            context,
                            "Fallo al subir imagen a Supabase",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

        })
    }

    private fun deleteImage(imageNumber: Int) {
        // Primero obtenemos los datos actualizados del usuario
        db.collection("usuarios")
            .whereEqualTo("e_mail", email)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    val user = document.toObject(Usuario::class.java)
                    if (user != null) {
                        Log.d("DeleteImage", "Intentando eliminar imagen $imageNumber")

                        // Guardamos la URL de la imagen a eliminar antes de hacer cualquier cambio
                        val imageToDelete = when (imageNumber) {
                            1 -> user.imagen1
                            2 -> user.imagen2
                            3 -> user.imagen3
                            else -> return@addOnSuccessListener
                        }

                        Log.d("DeleteImage", "URL a eliminar: $imageToDelete")

                        if (imageToDelete.isEmpty()) {
                            Log.d("DeleteImage", "La imagen está vacía, no hay nada que eliminar")
                            Toast.makeText(requireContext(), "No hay ninguna imagen para eliminar", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Primero eliminamos la imagen de Supabase
                        val bucket = "images"
                        val authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB0ZnZqc3doY253Y3lvb3NzZnJ0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDc0MDg4MDQsImV4cCI6MjA2Mjk4NDgwNH0.8K2P087GLNqcndYB_1FJW2tples19SUGb9t_5GByigk"

                        // Extraemos el nombre del archivo de la URL
                        val fileName = imageToDelete.substringAfterLast("/")
                        Log.d("DeleteImage", "Nombre del archivo a eliminar: $fileName")

                        val deleteCall = service.deleteImage(authHeader, bucket, fileName)
                        deleteCall.enqueue(object : Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                Log.d("DeleteImage", "Respuesta de Supabase: ${response.code()}")

                                // Después de eliminar la imagen de Supabase, actualizamos Firestore
                                val currentImages = mutableListOf(
                                    user.imagen1,
                                    user.imagen2,
                                    user.imagen3
                                )

                                Log.d("DeleteImage", "Imágenes actuales: $currentImages")

                                // Eliminamos la imagen seleccionada
                                currentImages[imageNumber - 1] = ""

                                // Reorganizamos las imágenes, moviendo las posteriores hacia la izquierda
                                val reorganizedImages = currentImages.filter { it.isNotEmpty() }
                                val finalImages = reorganizedImages.toMutableList()

                                // Aseguramos que siempre tengamos 3 elementos (rellenando con strings vacíos si es necesario)
                                while (finalImages.size < 3) {
                                    finalImages.add("")
                                }

                                Log.d("DeleteImage", "Imágenes reorganizadas: $finalImages")

                                // Actualizamos Firestore con las nuevas posiciones
                                val update = mapOf(
                                    "imagen1" to finalImages[0],
                                    "imagen2" to finalImages[1],
                                    "imagen3" to finalImages[2]
                                )

                                Log.d("DeleteImage", "Actualizando Firestore con: $update")

                                document.reference.update(update)
                                    .addOnSuccessListener {
                                        // Actualizamos las vistas de las imágenes
                                        Glide.with(this@ProfileFragment)
                                            .load(finalImages[0].toUri())
                                            .into(binding.imv1)

                                        Glide.with(this@ProfileFragment)
                                            .load(finalImages[1].toUri())
                                            .into(binding.imv2)

                                        Glide.with(this@ProfileFragment)
                                            .load(finalImages[2].toUri())
                                            .into(binding.imv3)

                                        Toast.makeText(requireContext(), "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.e("DeleteImage", "Error al eliminar imagen", t)
                                Toast.makeText(requireContext(), "Error al eliminar la imagen", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteImage", "Error al obtener datos del usuario", e)
                Toast.makeText(requireContext(), "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startPeriodicUpdates() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    loadUserProfile(email)
                }
            }
        }, 0, 2000) // Actualiza cada 2 segundos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
        _binding = null
    }
}