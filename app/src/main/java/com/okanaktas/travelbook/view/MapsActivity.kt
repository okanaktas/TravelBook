package com.okanaktas.travelbook.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Dao
import androidx.room.Room

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.okanaktas.travelbook.R
import com.okanaktas.travelbook.databinding.ActivityMapsBinding
import com.okanaktas.travelbook.model.Place
import com.okanaktas.travelbook.roomdb.PlaceDao
import com.okanaktas.travelbook.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolen: Boolean? = null
    private lateinit var db: PlaceDatabase
    private lateinit var placeDao: PlaceDao
    val compositeDisposable = CompositeDisposable()
    var placeFromMain: Place? = null

    //Uzun basımlarda konumu almak icin degiskenler
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    //izin istemek icin tanımlıyorum
    private lateinit var permissinLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()

        sharedPreferences = this.getSharedPreferences("com.okanaktas.travelbook", MODE_PRIVATE)
        trackBoolen = false

        selectedLatitude = 0.0
        selectedLongitude = 0.0

        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places")
            //allowMainThreadQueries ana thread de calismasina izin ver
            //.allowMainThreadQueries()
            .build()

        placeDao = db.placeDao()

        binding.saveButton.isEnabled = false
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")

        if (info == "new") {
            //Butonlarda degisiklik
            binding.saveButton.visibility = View.VISIBLE
            //GONE -> Hem gorunmez yapar hemde yer tutmaz, INVISIBLE -> Gorunmez yapar ama yerini tutar
            binding.deleteButton.visibility = View.GONE
            //kullanıcı yeni yer ekliyor

            //locationManager -> konum yoneticimiz, konumla ilgili tum islemleri ele alıyor.
            //locationLisetener -> Konum degisikliklerini dinleyen ve bize haber veren öge, arayuz.

            //casting -> kullanılan servis(LOCATIN_SERVISE) bir LocationManager olduguna eminim.
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {

                    //eger trackBoolean false ise bir defaya mahsus onLocationChanged'i calistir. eger degilse calsitirma

                    //bu deger sharedPreferences de kayıtlı mı ona bakıcam. Kayıtlı degilse ilk deger false olarak kalsin
                    trackBoolen = sharedPreferences.getBoolean("trackBoolean", false)

                    //ilk kez calismis ise bu calissin
                    if (trackBoolen == false) {
                        val userLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                        //Daha sonrasında trackBoolean'ı true yapıyorum ki her defasında cagrılmasın.
                        sharedPreferences.edit().putBoolean("trackBoolean", true).apply()
                    }

                }
            }

            //İznin verilip verilmedigini kontrol ettimiz yer
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //İzin isteme kısmı
                //Kullanıcyı bununla ilgili bir mesaj göstermeli miyiz bunu kontrol ediyoruz. Eger bu true donerse kullanıcıya bir mesah gostermemiz gerekiyor ve bununla birlikte izni istememiz gerekiyor. False donerse de izin isteyecegiz ama bunu android kendi belirliyor.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this@MapsActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(binding.root, "Permission needed!", Snackbar.LENGTH_INDEFINITE).setAction("Give permission") {
                        //request permission
                        permissinLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }.show()
                } else {
                    //request permission
                    permissinLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            } else {
                //permissions granted
                //kullanıcının konumunu aldıgımız yer
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

                //Kullanıcının son konumunu alıyoruz
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                //lastLocation null dondugu icin(boyle bir konum olmayabilir cunku) if dongusune sokuyoruz
                if (lastLocation != null) {
                    val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                }
                //izin verildiyse konumumu goster
                mMap.isMyLocationEnabled = true
            }


        } else {
            //eklenmis var olan yer

            mMap.clear()

            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place

            //null degil ise anlamına geliyor ?.let
            placeFromMain?.let {
                val latlng = LatLng(it.latitude, it.longitude)
                mMap.addMarker(MarkerOptions().position(latlng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16f))

                binding.placeText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }


        }


    }

    //
    private fun registerLauncher() {

        //registerLacunher kısmı android tarafından hazır olarak veriliyor ve sonucunda bir boolean donuyor (izin verildi ya da verilmedi)
        permissinLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                //Android izin verildigine emin olunmasını istedigi icin bir kez daha kontrol ediyoruz
                //permission granted
                if (ContextCompat.checkSelfPermission(this@MapsActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

                    //Kullanıcının son konumunu alıyoruz
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                    //lastLocation null dondugu icin(boyle bir konum olmayabilir cunku) if dongusune sokuyoruz
                    if (lastLocation != null) {
                        val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                    }

                    //izni ilk defa aldıgımızda konumumu goster
                    mMap.isMyLocationEnabled = true
                }
            } else {
                //permission denied
                Toast.makeText(this@MapsActivity, "Permission Needed!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapLongClick(p0: LatLng) {

        //daha once eklenmis bir marker varsa onu siler
        mMap.clear()

        //Uzun Tıklandıgı zaman marker ekleme
        mMap.addMarker(MarkerOptions().position(p0))

        //secilen yerleri selected olan degiskenlere atadık
        selectedLatitude = p0.latitude
        selectedLongitude = p0.longitude

        binding.saveButton.isEnabled = true
    }

    fun buttonSave(view: View) {

        if (selectedLatitude != null && selectedLongitude != null) {
            val place = Place(binding.placeText.text.toString(), selectedLatitude!!, selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResponse)
            )
        }

    }

    private fun handleResponse() {
        val intent = Intent(this@MapsActivity, MainActivity::class.java)
        //Acik olan butun activiteleri kapat
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun buttonDelete(view: View) {

        //placeFromMain null degilse anlamında ?.let
        placeFromMain?.let {

            compositeDisposable.add(
                placeDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        compositeDisposable.clear()
    }
}