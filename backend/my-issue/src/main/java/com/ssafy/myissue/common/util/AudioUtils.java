package com.ssafy.myissue.common.util;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioUtils {
    // WAV 파일의 길이를 초 단위로 반환
    public static double getWavDurationinSeconds(byte[] wavBytes) {
        try (
                InputStream byteArrayInputStream = new ByteArrayInputStream(wavBytes); // 바이트 배열을 InputStream으로 변환
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(byteArrayInputStream)) // InputStream을 해석해서 오디오 스트림
        {
            // AudioFormat : 오디오 데이터의 형식에 대한 정보(샘플링 레이트, 비트 깊이, 채널 수 등)
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength(); // 전체 오디오 스트림이 몇 개의 프레임(샘플 단위 묶음)으로 되어 있는지 반환.
            return (frames + 0.0) / format.getFrameRate(); // 전체 프레임 수(long 변환) ÷ 초당 프레임 수 = 전체 길이(초)
        } catch (Exception e) {
            throw new RuntimeException("WAV 길이 계산 실패", e);
        }
    }

    // WAV 파일 합치기
    public static byte[] mergeWavFiles(List<byte[]> wavFiles){
        try {
            List<AudioInputStream> audioStream = new ArrayList<>();

            // 입력된 bytes[] 리스트를 AudioInputStream 리스트로 변환
            for(byte[] wav : wavFiles){
                audioStream.add(AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav)));
            }

            AudioFormat format = audioStream.get(0).getFormat(); // 첫 번째 WAV 파일의 오디오 포맷 정보를 가져옴. ( 모든 파일이 같은 포맷이어야 정상 작동 )
            AudioInputStream appendedFiles = new AudioInputStream(
                new SequenceInputStream(Collections.enumeration(audioStream.stream()
                        .map(s -> (InputStream) s)
                        .toList())),
                    format,
                    audioStream.stream().mapToLong(AudioInputStream::getFrameLength).sum()
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream(); // byte[]
            AudioSystem.write(appendedFiles, AudioFileFormat.Type.WAVE, baos); // AudioInputStream을 WAV 형식으로 씀
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("WAV 파일 합치기 실패", e);
        }
    }
}
