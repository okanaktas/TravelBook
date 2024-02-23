package com.okanaktas.travelbook.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.okanaktas.travelbook.R
import com.okanaktas.travelbook.adapter.PlaceAdapter
import com.okanaktas.travelbook.databinding.ActivityMainBinding
import com.okanaktas.travelbook.model.Place
import com.okanaktas.travelbook.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val db = Room.databaseBuilder(applicationContext,PlaceDatabase::class.java,"Places").build()
        val placeDao = db.placeDao()

        compositeDisposable.add(
            placeDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )


    }

    private fun handleResponse(placeList : List<Place>){
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PlaceAdapter(placeList)
        binding.recyclerView.adapter = adapter
    }

    //olusturdugumuz menuyu ana aktivitemiz ile bagladımızı kısım
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //ne zaman bir xml ile kodu baglacayacagız inflater ile calisiyoruz
        val menuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.place_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //o menuden herhangi bir eleman secilirse ne olacıgını yazdıgımız kısım
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //su an menu icerisinde bir tane item var yine de kontrol ediyoruz hangi item seciliyor diye
        if (item.itemId == R.id.add_place) {
            val intent = Intent(this@MainActivity, MapsActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}