package ru.migger.minio;


import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import org.apache.maven.wagon.*;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MinioWagon extends StreamWagon {
    private volatile MinioClient minioClient;
    private volatile String bucketName;
    private volatile String baseDirectory;
    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        if (minioClient == null) {
            try {
                {
                    String user = System.getenv("MINIO_USER");
                    if (user == null) {
                        user = System.getProperty("minio.user");
                        if (user == null) {
                            user = authenticationInfo.getUserName();
                            if (user == null) {
                                user = "minio";
                            }
                        }
                    }
                    String password = System.getenv("MINIO_PASSWORD");
                    if (password == null) {
                        password = System.getProperty("minio.password");
                        if (password == null) {
                            password = authenticationInfo.getPassword();
                            if (password == null) {
                                password = "minio123";
                            }
                        }
                    }

                    final int atIndex = user.indexOf('@');
                    if (atIndex < 0)
                        throw new AuthenticationException("use notation user@hostname to specify minio server");

                    String hostname = user.substring(atIndex + 1);
                    user = user.substring(0, atIndex);

                    this.bucketName = repository.getHost();
                    this.baseDirectory = repository.getBasedir() + (repository.getBasedir().endsWith("/") ? "" : "/");
                    minioClient = new MinioClient("http://" + hostname, user, password);

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void fillInputData(InputData inputData) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            inputData.setInputStream(minioClient.getObject(this.bucketName, this.baseDirectory + inputData.getResource().getName()));
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new ResourceDoesNotExistException(e.getMessage(), e);
            }
            throw new TransferFailedException(e.getMessage(), e);
        } catch (Exception e) {
            throw new TransferFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void fillOutputData(OutputData outputData) throws TransferFailedException {
        outputData.setOutputStream(new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                try {
                    minioClient.putObject(bucketName, baseDirectory + outputData.getResource().getName(), new ByteArrayInputStream(toByteArray()), null);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        });
    }

    @Override
    public void closeConnection() throws ConnectionException {
        minioClient = null;
        bucketName = null;
        baseDirectory = null;
    }
}
