package com.MWS.storage;

import com.MWS.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы с S3-совместимым хранилищем (Ceph, MinIO и т.д.)
 */
@Component
public class S3FileStorage {

    private static final Logger logger = LoggerFactory.getLogger(S3FileStorage.class);
    private final S3Client s3Client;
    private final String bucketName;

    public S3FileStorage() {
        this.bucketName = Config.getCephBucketName();

        // Создаем credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                Config.getCephAccessKey(),
                Config.getCephSecretKey()
        );

        // Создаем S3 клиент для Ceph
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(Config.getCephEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1) // Для Ceph регион не важен, но обязателен
                .forcePathStyle(true) // Важно для Ceph/MinIO
                .build();

        logger.info("S3FileStorage инициализирован. Endpoint: {}, Bucket: {}",
                Config.getCephEndpoint(), bucketName);

        // Создаем bucket если не существует
        createBucketIfNotExists();
    }

    /**
     * Создает bucket если он не существует
     */
    private void createBucketIfNotExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            logger.info("Bucket '{}' уже существует", bucketName);

        } catch (NoSuchBucketException e) {
            logger.info("Bucket '{}' не найден, создаем...", bucketName);
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();

                s3Client.createBucket(createBucketRequest);
                logger.info("✅ Bucket '{}' успешно создан", bucketName);

            } catch (Exception ex) {
                logger.error("❌ Не удалось создать bucket '{}'", bucketName, ex);
                throw new RuntimeException("Не удалось создать bucket", ex);
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке bucket", e);
            throw new RuntimeException("Ошибка подключения к S3/Ceph", e);
        }
    }

    /**
     * Загружает файл в S3/Ceph
     *
     * @param s3Key Ключ файла в S3
     * @param inputStream Поток данных файла
     * @param contentLength Размер файла
     * @param contentType MIME-тип файла
     * @return URL загруженного файла
     */
    public String uploadFile(String s3Key, InputStream inputStream, long contentLength, String contentType) {
        try {
            logger.info("Загрузка файла в S3: {} ({} байт, тип: {})", s3Key, contentLength, contentType);

            // Читаем весь поток в память (для небольших файлов)
            // Для больших файлов лучше использовать multipart upload
            byte[] fileBytes = inputStream.readAllBytes();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            String fileUrl = String.format("%s/%s/%s", Config.getCephEndpoint(), bucketName, s3Key);
            logger.info("✅ Файл успешно загружен: {}", fileUrl);

            return fileUrl;

        } catch (IOException e) {
            logger.error("❌ Ошибка чтения файла для загрузки в S3: {}", s3Key, e);
            throw new RuntimeException("Не удалось прочитать файл", e);
        } catch (S3Exception e) {
            logger.error("❌ Ошибка S3 при загрузке файла: {}", s3Key, e);
            throw new RuntimeException("Ошибка загрузки в S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Неожиданная ошибка при загрузке файла: {}", s3Key, e);
            throw new RuntimeException("Не удалось загрузить файл в S3", e);
        }
    }

    /**
     * Скачивает файл из S3/Ceph
     *
     * @param s3Key Ключ файла в S3
     * @return Поток данных файла
     */
    public InputStream downloadFile(String s3Key) {
        try {
            logger.info("Скачивание файла из S3: {}", s3Key);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            // Получаем файл как byte array
            byte[] fileBytes = s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes()).asByteArray();

            logger.info("✅ Файл успешно скачан из S3: {} ({} байт)", s3Key, fileBytes.length);

            return new ByteArrayInputStream(fileBytes);

        } catch (NoSuchKeyException e) {
            logger.error("❌ Файл не найден в S3: {}", s3Key);
            throw new RuntimeException("Файл не найден в хранилище");
        } catch (S3Exception e) {
            logger.error("❌ Ошибка S3 при скачивании файла: {}", s3Key, e);
            throw new RuntimeException("Ошибка скачивания из S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Неожиданная ошибка при скачивании файла: {}", s3Key, e);
            throw new RuntimeException("Не удалось скачать файл из S3", e);
        }
    }

    /**
     * Удаляет файл из S3/Ceph
     *
     * @param s3Key Ключ файла в S3
     */
    public void deleteFile(String s3Key) {
        try {
            logger.info("Удаление файла из S3: {}", s3Key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            logger.info("✅ Файл успешно удален из S3: {}", s3Key);

        } catch (S3Exception e) {
            logger.error("❌ Ошибка S3 при удалении файла: {}", s3Key, e);
            throw new RuntimeException("Ошибка удаления из S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Неожиданная ошибка при удалении файла: {}", s3Key, e);
            throw new RuntimeException("Не удалось удалить файл из S3", e);
        }
    }

    /**
     * Проверяет существование файла в S3
     *
     * @param s3Key Ключ файла в S3
     * @return true если файл существует
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Ошибка при проверке существования файла: {}", s3Key, e);
            return false;
        }
    }

    /**
     * Получает список всех файлов пользователя в S3
     *
     * @param userPrefix Префикс пользователя (например "user/123/")
     * @return Список ключей файлов
     */
    public List<String> listFiles(String userPrefix) {
        try {
            logger.info("Получение списка файлов с префиксом: {}", userPrefix);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(userPrefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            List<String> fileKeys = new ArrayList<>();

            for (S3Object s3Object : listResponse.contents()) {
                fileKeys.add(s3Object.key());
            }

            logger.info("✅ Найдено {} файлов с префиксом: {}", fileKeys.size(), userPrefix);
            return fileKeys;

        } catch (S3Exception e) {
            logger.error("❌ Ошибка S3 при получении списка файлов", e);
            throw new RuntimeException("Ошибка получения списка файлов: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            logger.error("❌ Неожиданная ошибка при получении списка файлов", e);
            throw new RuntimeException("Не удалось получить список файлов", e);
        }
    }

    /**
     * Получает размер файла в S3
     *
     * @param s3Key Ключ файла в S3
     * @return Размер файла в байтах
     */
    public long getFileSize(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.contentLength();

        } catch (NoSuchKeyException e) {
            throw new RuntimeException("Файл не найден в хранилище");
        } catch (Exception e) {
            logger.error("Ошибка при получении размера файла: {}", s3Key, e);
            throw new RuntimeException("Не удалось получить размер файла", e);
        }
    }

    /**
     * Проверяет подключение к S3/Ceph
     *
     * @return true если подключение успешно
     */
    public boolean testConnection() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();
            logger.info("✅ Подключение к S3/Ceph успешно. Найдено buckets: {}", response.buckets().size());
            return true;
        } catch (Exception e) {
            logger.error("❌ Не удалось подключиться к S3/Ceph", e);
            return false;
        }
    }

    /**
     * Закрывает S3 клиент
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3 клиент закрыт");
        }
    }
}