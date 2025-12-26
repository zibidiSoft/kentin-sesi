package io.github.thwisse.kentinsesi.data.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.GeoPoint

/**
 * GeoPoint için Parcelable wrapper
 * Firebase GeoPoint Parcelable olmadığı için custom wrapper oluşturduk
 */
data class ParcelableGeoPoint(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    
    constructor(geoPoint: GeoPoint) : this(geoPoint.latitude, geoPoint.longitude)
    
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }
    
    override fun describeContents(): Int = 0
    
    companion object CREATOR : Parcelable.Creator<ParcelableGeoPoint> {
        override fun createFromParcel(parcel: Parcel): ParcelableGeoPoint {
            return ParcelableGeoPoint(parcel)
        }
        
        override fun newArray(size: Int): Array<ParcelableGeoPoint?> {
            return arrayOfNulls(size)
        }
    }
}

