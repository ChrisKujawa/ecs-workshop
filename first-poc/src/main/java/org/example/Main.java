package org.example;

import org.agrona.concurrent.UnsafeBuffer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.lang.System.exit;

public class Main {
    public static void main(String[] args) {
        final var path = System.getenv("CAMUNDA_PATH");
        if (path == null) {
            throw new IllegalArgumentException("Please set CAMUNDA_PATH environment variable pointing to the folder to write.");
        }
        System.out.println("Will write into: " + path);

        final var runtimeSecEnv = System.getenv("CAMUNDA_RUNTIME_SEC");
        var runtimeSec = 500l;
        if (runtimeSecEnv != null) {
            long seconds = Long.parseLong(runtimeSecEnv);
            runtimeSec = seconds;
        }
        final var loops = Math.max(runtimeSec / 5, 1);
        System.out.printf("Will loop for %d times a 5 seconds.\n", loops);

        System.out.println("Attempt to acquire lock...");
        try (final FileOutputStream fileOutputStream = new FileOutputStream(path + "/lockfile");
             final var channel = fileOutputStream.getChannel()) {
            // lock inside the try (so finalize will not close it)
            final var lock = channel.lock();
            System.out.println("Lock acquired!");

            for (int i =0; i < loops; i++) {
                final var filePath = path + "/append-file.txt";
                try (var appendChannel = FileChannel.open(Path.of(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    // write to channel
                    long currentTimeMillis = System.currentTimeMillis();
                    var buffer = new UnsafeBuffer(ByteBuffer.allocate(256));
                    buffer.putStringUtf8(0, "Current time: " + currentTimeMillis + "\n");
                    System.out.printf("[%d/%d]: Write %d into: %s.\n", i+1, loops, currentTimeMillis, filePath);
                    appendChannel.write(buffer.byteBuffer(), 0);
                }
                Thread.sleep(5000);
            }
            throw new Error("");
//            lock.release();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}