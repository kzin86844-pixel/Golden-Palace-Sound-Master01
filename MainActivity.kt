package com.goldenpalace.soundmaster

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

class MainActivity : Activity() {
 private var music: MediaPlayer?=null
 private var uri: Uri?=null
 private var rec: AudioRecord?=null
 private var out: AudioTrack?=null
 @Volatile private var micOn=false
 @Volatile private var micGain=.65f
 private var musicGain=.75f
 private lateinit var status: TextView
 private val picker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
  if(r.resultCode==RESULT_OK){ uri=r.data?.data; status.text=if(uri!=null) "Music file selected ✓" else "No file selected"
   uri?.let { contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
 }
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); window.statusBarColor=0xff09101f.toInt(); window.navigationBarColor=0xff09101f.toInt(); ui()}
 private fun ui(){
  val root=LinearLayout(this).apply{orientation=1;setPadding(24,28,24,24);setBackgroundColor(0xff09101f.toInt())}
  fun label(s:String,size:Float,color:Int= -1,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size;setTextColor(color);if(bold)setTypeface(null,1);setPadding(0,8,0,8)}
  fun add(v:android.view.View){root.addView(v,LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT).apply{bottomMargin=8})}
  val gold=0xffdcb55b.toInt()
  add(label("GOLDEN PALACE",25f,gold,true)); add(label("SOUND MASTER • LIVE MIXER",13f,0xffdddddd.toInt(),true))
  status=label("Ready — choose a music file",14f,gold);add(status)
  add(Button(this).apply{text="CHOOSE MUSIC FILE";setOnClickListener{val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};picker.launch(i)}})
  add(Button(this).apply{text="PLAY MUSIC";setOnClickListener{playMusic()}})
  add(label("MUSIC VOLUME",14f,gold,true))
  add(SeekBar(this).apply{max=100;progress=75;setOnSeekBarChangeListener(slider{musicGain=it/100f;music?.setVolume(musicGain,musicGain)})})
  add(label("MICROPHONE GAIN",14f,gold,true))
  add(SeekBar(this).apply{max=100;progress=65;setOnSeekBarChangeListener(slider{micGain=it/100f})})
  val mic=Button(this).apply{text="START MICROPHONE";setOnClickListener{if(micOn){stopMic();text="START MICROPHONE";status.text="Microphone stopped"}else if(ActivityCompat.checkSelfPermission(this@MainActivity,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this@MainActivity,arrayOf(Manifest.permission.RECORD_AUDIO),20)}else{startMic();text="STOP MICROPHONE"}}}
  add(mic)
  add(Button(this).apply{text="STOP ALL";setOnClickListener{music?.release();music=null;stopMic();mic.text="START MICROPHONE";status.text="Stopped"}})
  add(label("Use wired headphones to reduce feedback. Bluetooth microphone monitoring may have delay.",11f,0xffcccccc.toInt()))
  setContentView(root)
 }
 private fun slider(f:(Int)->Unit)=object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){f(p)};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}}
 private fun playMusic(){val u=uri;if(u==null){Toast.makeText(this,"Choose a music file first",0).show();return};try{music?.release();music=MediaPlayer().apply{setDataSource(this@MainActivity,u);setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());setOnPreparedListener{it.setVolume(musicGain,musicGain);it.start();status.text="Music playing"};prepareAsync()}}catch(e:Exception){status.text="Could not play this file"}}
 private fun startMic(){if(micOn)return;try{val rate=44100;val bi=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);val bo=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);rec=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bi*2);out=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(bo*2).setTransferMode(AudioTrack.MODE_STREAM).build();micOn=true;rec?.startRecording();out?.play();Thread{Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);val b=ShortArray(1024);while(micOn){val n=rec?.read(b,0,b.size)?:0;if(n>0&&micOn){val g=micGain;for(i in 0 until n)b[i]=(b[i]*g).toInt().coerceIn(-32768,32767).toShort();out?.write(b,0,n)}}}.start();status.text="Music + microphone active"}catch(e:Exception){micOn=false;releaseMic();status.text="Microphone could not start"}}
 private fun stopMic(){micOn=false;try{rec?.stop()}catch(_:Exception){};try{out?.pause();out?.flush();out?.stop()}catch(_:Exception){};releaseMic()}
 private fun releaseMic(){try{rec?.release()}catch(_:Exception){};try{out?.release()}catch(_:Exception){};rec=null;out=null}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==20&&g.isNotEmpty()&&g[0]==PackageManager.PERMISSION_GRANTED)startMic()else if(r==20)status.text="Microphone permission denied"}
 override fun onDestroy(){stopMic();music?.release();super.onDestroy()}
}
