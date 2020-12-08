package org.odk.collect.audiorecorder.recorder

import com.google.common.io.Files
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Test
import java.io.File
import java.io.IOException

class RecordingResourceRecorderTest {

    private val cacheDir = Files.createTempDir()
    private val recordingResource = FakeRecordingResource()

    private var lastOutput: Output? = null
    private val recorder = RecordingResourceRecorder(cacheDir) { output ->
        lastOutput = output
        recordingResource
    }

    @Test
    fun start_startsMediaRecorder() {
        recorder.start(Output.AAC)
        assertThat(recordingResource.hasStarted(), equalTo(true))
    }

    @Test
    fun start_withAAC_setsUpAACRecording() {
        recorder.start(Output.AAC)
        assertThat(lastOutput, equalTo(Output.AAC))
    }

    @Test
    fun start_withAMR_setsUpAMRRecording() {
        recorder.start(Output.AMR)
        assertThat(lastOutput, equalTo(Output.AMR))
    }

    @Test
    fun start_createsAndRecordsToM4AFileInCacheDir() {
        recorder.start(Output.AAC)
        assertThat(recordingResource.getOutputFile()!!.parent, equalTo(cacheDir.absolutePath))
        assertThat(recordingResource.getOutputFile()!!.absolutePath, endsWith(".m4a"))
    }

    @Test
    fun start_createsAndRecordsToAMRFileInCacheDir() {
        recorder.start(Output.AMR)
        assertThat(recordingResource.getOutputFile()!!.parent, equalTo(cacheDir.absolutePath))
        assertThat(recordingResource.getOutputFile()!!.absolutePath, endsWith(".amr"))
    }

    @Test
    fun start_setsIsRecording_toTrue() {
        recorder.start(Output.AAC)
        assertThat(recorder.isRecording(), equalTo(true))
    }

    @Test
    fun recordingTwice_doesntUseSameOutputFile() {
        var mediaRecorder = FakeRecordingResource()
        var recorder = RecordingResourceRecorder(cacheDir) { mediaRecorder }
        recorder.start(Output.AAC)
        val outputFile1 = mediaRecorder.getOutputFile()

        mediaRecorder = FakeRecordingResource()
        recorder = RecordingResourceRecorder(cacheDir) { mediaRecorder }
        recorder.start(Output.AAC)
        val outputFile2 = mediaRecorder.getOutputFile()

        assertThat(outputFile1!!.absolutePath, not(equalTo(outputFile2!!.absolutePath)))
    }

    @Test(expected = RecordingException::class)
    fun start_whenFileCantBeCreated_throwsRecordingException() {
        cacheDir.deleteRecursively()
        recorder.start(Output.AAC)
    }

    @Test(expected = RecordingException::class)
    fun start_whenPrepareFails_throwsRecordingException() {
        recordingResource.failOnPrepare()
        recorder.start(Output.AAC)
    }

    @Test
    fun stop_releasesMediaRecorder() {
        recorder.start(Output.AAC)
        recorder.stop()
        assertThat(recordingResource.isReleased(), equalTo(true))
    }

    @Test
    fun stop_returnsOutputFile() {
        recorder.start(Output.AAC)
        val file = recorder.stop()
        assertThat(file.absolutePath, equalTo(recordingResource.getOutputFile()!!.absolutePath))
    }

    @Test
    fun stop_setsIsRecording_toFalse() {
        recorder.start(Output.AAC)
        recorder.stop()
        assertThat(recorder.isRecording(), equalTo(false))
    }

    @Test
    fun cancel_releasesMediaRecorder() {
        recorder.start(Output.AAC)
        recorder.cancel()
        assertThat(recordingResource.isReleased(), equalTo(true))
    }

    @Test
    fun cancel_deletesOutputFile() {
        recorder.start(Output.AAC)
        recorder.cancel()
        assertThat(recordingResource.getOutputFile()!!.exists(), equalTo(false))
    }

    @Test
    fun cancel_setsIsRecording_toFalse() {
        recorder.start(Output.AAC)
        recorder.cancel()
        assertThat(recorder.isRecording(), equalTo(false))
    }

    @Test
    fun cancel_beforeStart_works() {
        recorder.cancel()
    }

    @Test
    fun pause_pausesMediaRecorder() {
        recorder.start(Output.AAC)
        recorder.pause()
        assertThat(recordingResource.isPaused(), equalTo(true))
    }

    @Test
    fun resume_resumesMediaRecorder() {
        recorder.start(Output.AAC)
        recorder.pause()
        recorder.resume()
        assertThat(recordingResource.isPaused(), equalTo(false))
    }
}

private class FakeRecordingResource : RecordingResource {

    private var file: File? = null

    private var started: Boolean = false
    private var prepared: Boolean = false
    private var released: Boolean = false
    private var paused: Boolean = false
    private var failOnPrepare: Boolean = false

    override fun setOutputFile(path: String) {
        if (prepared) {
            throw IllegalStateException("MediaRecorder already prepared!")
        }

        if (!File(path).exists()) {
            throw IllegalArgumentException("Path doesn't refer to created file!")
        }

        file = File(path)
    }

    @Throws(IOException::class)
    override fun prepare() {
        if (failOnPrepare) {
            throw IOException()
        } else {
            prepared = true
        }
    }

    override fun start() {
        if (!prepared) {
            throw IllegalStateException("MediaRecorder not prepared!")
        }

        started = true
    }

    override fun pause() {
        if (!started) {
            throw IllegalStateException("MediaRecorder not started!")
        }

        paused = true
    }

    override fun resume() {
        if (!started) {
            throw IllegalStateException("MediaRecorder not started!")
        }

        paused = false
    }

    override fun stop() {
        started = false
    }

    override fun release() {
        if (started) {
            throw IllegalStateException("Cannot release while recording! Call stop() first")
        }

        released = true
    }

    override fun getMaxAmplitude(): Int {
        return 0
    }

    fun hasStarted(): Boolean {
        return started
    }

    fun getOutputFile(): File? {
        return file
    }

    fun isReleased(): Boolean {
        return released
    }

    fun isPaused(): Boolean {
        return paused
    }

    fun failOnPrepare() {
        failOnPrepare = true
    }
}