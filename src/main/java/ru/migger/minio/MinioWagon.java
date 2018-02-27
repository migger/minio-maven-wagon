package ru.migger.minio;


import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.TransferListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MinioWagon extends AbstractWagon {
    private volatile MinioClient minioClient;
    private volatile String bucketName;
    private volatile String baseDirectory;
    private final List<TransferListener> transferListeners = new ArrayList<>();

    @Override
    protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
        if (minioClient == null) {
            sessionEventSupport.fireSessionOpening(new SessionEvent(this, SessionEvent.SESSION_OPENING));
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
    protected void closeConnection() throws ConnectionException {
        this.minioClient = null;
        this.bucketName = null;
        this.baseDirectory = null;
    }

    @Override
    public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            minioClient.getObject(this.bucketName, this.baseDirectory + resourceName, destination.getAbsolutePath());
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
    public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            if (minioClient.statObject(this.bucketName, this.baseDirectory + resourceName).createdTime().after(new Date(timestamp))) {
                return false;
            }
            minioClient.getObject(this.bucketName, this.baseDirectory + resourceName, destination.getAbsolutePath());
            return true;
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
    public void put(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            minioClient.putObject(this.bucketName, this.baseDirectory + destination, source.getAbsolutePath());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new ResourceDoesNotExistException(e.getMessage(), e);
            }
            throw new TransferFailedException(e.getMessage(), e);
        } catch (Exception e) {
            throw new TransferFailedException(e.getMessage(), e);
        }

    }
}
