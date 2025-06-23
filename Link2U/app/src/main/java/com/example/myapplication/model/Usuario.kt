package com.example.myapplication.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp
import java.sql.Date

data class Usuario(
    val e_mail: String = "",
    var contraseña: String = "",
    var edad: Int = 0,
    var nombre_usuario: String = "",
    var fecha_creacion: Timestamp = Timestamp.now(),
    var imagen_perfil: String = "",
    var sexo_usuario: String = "",
    var redes_sociales: List<String> = emptyList(),
    var imagen1: String = "",
    var imagen2: String = "",
    var imagen3: String = "",
    var provider: String= ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        Timestamp(Date(parcel.readLong())),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArray()?.toList() ?: emptyList(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(e_mail)
        parcel.writeString(contraseña)
        parcel.writeInt(edad)
        parcel.writeString(nombre_usuario)
        parcel.writeLong(fecha_creacion.toDate().time)
        parcel.writeString(imagen_perfil)
        parcel.writeString(sexo_usuario)
        parcel.writeStringArray(redes_sociales.toTypedArray())
        parcel.writeString(imagen1)
        parcel.writeString(imagen2)
        parcel.writeString(imagen3)
        parcel.writeString(provider)

    }

    override fun describeContents(): Int = 0
    override fun toString(): String {
        return "Usuario(e_mail='$e_mail', edad=$edad, nombre_usuario='$nombre_usuario', fecha_creacion=$fecha_creacion, sexo_usuario='$sexo_usuario', redes_sociales=$redes_sociales)"
    }

    companion object CREATOR : Parcelable.Creator<Usuario> {
        override fun createFromParcel(parcel: Parcel): Usuario {
            return Usuario(parcel)
        }

        override fun newArray(size: Int): Array<Usuario?> {
            return arrayOfNulls(size)
        }
    }

} 