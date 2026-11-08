package uz.alesle.avto.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import uz.alesle.avto.shell.ShellContext
import uz.alesle.avto.shell.ShellContexts
import java.io.FileOutputStream
import kotlin.system.exitProcess

/**
 * Runs in a separate process that Shizuku starts with adb (shell) rights, which may record the
 * "remote submix": everything the phone plays, navigation voice included. While it is recorded
 * the phone itself goes quiet, as it does with Android Auto. This is how scrcpy captures audio.
 *
 * Android 12+ only (the recording must be attributed to the shell, which needs AudioRecord's
 * setContext).
 */
class ShellAudioService : IShellAudio.Stub {
    private val context: Context?
    @Volatile private var record: AudioRecord? = null
    private var pump: Thread? = null

    constructor() : this(null)

    /** Shizuku passes a Context when the service has this constructor. */
    constructor(context: Context?) {
        this.context = context
    }

    // The shell holds RECORD_AUDIO and CAPTURE_AUDIO_OUTPUT; REMOTE_SUBMIX isn't in AudioRecord's public list.
    @SuppressLint("MissingPermission", "WrongConstant")
    override fun start(sampleRate: Int, channels: Int): ParcelFileDescriptor? {
        stop()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO)
                .build()
            val chunk = sampleRate / 50 * channels * 2
            val r = AudioRecord.Builder()
                .setContext(ShellContext(context ?: ShellContexts.systemContext()))
                .setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
                .setAudioFormat(format)
                .setBufferSizeInBytes(maxOf(chunk * 8, AudioRecord.getMinBufferSize(sampleRate, format.channelMask, format.encoding)))
                .build()
            r.startRecording()
            record = r
            val (read, write) = ParcelFileDescriptor.createPipe()
            pump = Thread({
                val buf = ByteArray(chunk)
                FileOutputStream(write.fileDescriptor).use { out ->
                    try {
                        while (record === r) {
                            val n = r.read(buf, 0, buf.size)
                            if (n < 0) break
                            if (n > 0) out.write(buf, 0, n)
                        }
                    } catch (_: Exception) {
                        // AARE closed its end of the pipe
                    }
                }
                write.close()
            }, "aare-shell-audio").apply { start() }
            read
        } catch (e: Exception) {
            Log.w(TAG, "remote submix capture failed", e)
            stop()
            null
        }
    }

    override fun stop() {
        val r = record ?: return
        record = null
        runCatching { r.stop() }
        pump?.join(500)
        pump = null
        r.release()
    }

    override fun destroy() {
        stop()
        exitProcess(0)
    }

    private companion object {
        const val TAG = "AareShellAudio"
    }
}
