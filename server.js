const express = require('express');
const cors = require('cors');
const ytdl = require('@distube/ytdl-core');
const { spawn } = require('child_process');
const ffmpegPath = require('ffmpeg-static');
const path = require('path');

const app = express();
app.use(cors());

// Serve the frontend page at "/"
app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'index.html')));

// Get video info
app.get('/api/info', async (req, res) => {
  try {
    const info = await ytdl.getInfo(req.query.url);
    res.json({
      title: info.videoDetails.title,
      thumbnail: info.videoDetails.thumbnails.at(-1)?.url,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Download MP4 (highest video + audio merged)
app.get('/api/download/mp4', async (req, res) => {
  try {
    const url = req.query.url;
    const video = ytdl(url, { filter: 'videoonly', quality: 'highest' });
    const audio = ytdl(url, { filter: 'audioonly', quality: 'highestaudio' });

    res.header('Content-Disposition', 'attachment; filename="video.mp4"');
    res.header('Content-Type', 'video/mp4');

    const ffmpeg = spawn(ffmpegPath, [
      '-i', 'pipe:3', '-i', 'pipe:4',
      '-c:v', 'copy', '-c:a', 'aac',
      '-f', 'mp4', 'pipe:5',
    ], { stdio: ['pipe', 'pipe', 'pipe', 'pipe', 'pipe'] });

    video.pipe(ffmpeg.stdio[3]);
    audio.pipe(ffmpeg.stdio[4]);
    ffmpeg.stdio[5].pipe(res);

    video.on('error', () => ffmpeg.kill('SIGKILL'));
    audio.on('error', () => ffmpeg.kill('SIGKILL'));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Download MP3 (audio only)
app.get('/api/download/mp3', (req, res) => {
  try {
    res.header('Content-Disposition', 'attachment; filename="audio.mp3"');
    res.header('Content-Type', 'audio/mpeg');
    ytdl(req.query.url, { filter: 'audioonly', quality: 'highestaudio' })
      .pipe(res);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.listen(process.env.PORT || 3000, () => console.log('Running'));
