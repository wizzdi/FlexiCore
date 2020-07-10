/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.service.impl;

import com.flexicore.annotations.IOperation;
import com.flexicore.constants.Constants;
import com.flexicore.data.BaselinkRepository;
import com.flexicore.data.FileResourceRepository;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.enums.ProcessPhase;
import com.flexicore.interfaces.AnalyzerPlugin;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.FinalizeFileResourceResponse;
import com.flexicore.rest.DownloadRESTService;
import com.flexicore.security.SecurityContext;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;

import javax.activation.MimetypesFileTypeMap;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import javax.inject.Named;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Primary
@Component
public class FileResourceService implements com.flexicore.service.FileResourceService {
    /**
     *
     */
    private static final long serialVersionUID = -2975140337610352781L;
    private static final int MAX_FILE_PART_SIZE = 2 * 1024 * 1024;

    //private String folder = "c:/temp/";

   private Logger logger = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private FileResourceRepository fileResourceRepository;

    @Autowired
    private JobService fileProcessJobService;

    @Autowired
    private BaseclassNewService baseclassNewService;

    @Autowired
    private BaselinkRepository baselinkRepository;

    @Autowired
    private JobService jobService;

    public FileResource getExistingFileResource(String md5, SecurityContext securityContext) {
        List<FileResource> existing = fileResourceRepository.listAllFileResources(new FileResourceFilter().setMd5s(Collections.singleton(md5)), securityContext);
        return existing.isEmpty() ? null : existing.get(0);
    }

    public FileResource uploadFileResource(String filename, SecurityContext securityContext, String md5, String chunkMd5, boolean lastChunk, InputStream fileInputStream) {

        FileResource fileResource = getExistingFileResource(md5, securityContext);
        if (fileResource == null) {
            fileResource = fileResourceRepository.create(filename, securityContext, md5);

        }
        if (!fileResource.isDone()) {
            saveFile(fileInputStream,chunkMd5, fileResource);
            if(lastChunk){
                File file = new File(fileResource.getFullPath());
                String calculatedFileMd5=generateMD5(file);
                if(!md5.equals(calculatedFileMd5)){
                    if(file.delete()){
                        fileResource.setOffset(0L);
                        fileResourceRepository.merge(fileResource);
                    }
                    else{
                        logger.warning("Could not delete bad md5 file "+file);

                    }
                    throw new ClientErrorException("File Total MD5 is "+calculatedFileMd5 +" expected "+md5, Response.Status.EXPECTATION_FAILED);
                }
                fileResource.setDone(true);
                fileResourceRepository.merge(fileResource);
            }

        }

        return fileResource;

    }


    public List<FileResource> getFileResourceScheduledForDelete(OffsetDateTime date) {
        return fileResourceRepository.getFileResourceScheduledForDelete(date);
    }

    /**
     * finalize upload, starts a new FC Job which invokes a {@link BatchRuntime}
     * Job. The created FC Job allows for a client tracking on {@link Job}
     * progress.
     *
     * @param md5
     * @param securityContext
     * @param hint
     * @return
     */

    public Job finalizeUpload(String md5, SecurityContext securityContext, String hint) {
        return finalizeUpload(md5, securityContext, hint, null);
    }

    private void createSecurityLinkForFileResource(SecurityContext securityContext, FileResource fileResource) {
        try {
            UserToBaseClass utb = new UserToBaseClass("userToBaseclass", securityContext);
            utb.setLeftside(securityContext.getUser());
            utb.setBaseclass(fileResource);
            String download = Baseclass.generateUUIDFromString
                    (DownloadRESTService.class.getMethod("download", String.class, String.class, SecurityContext.class).toString());
            Operation op = baselinkRepository.findById(Operation.class, download);

            utb.setValue(op);
            utb.setSimplevalue(IOperation.Access.allow.name());
            merge(utb);
        } catch (NoSuchMethodException | SecurityException e) {
            logger.log(Level.SEVERE, "unable to create security link for fileResource: " + fileResource, e);
        }
    }


    public FinalizeFileResourceResponse finalize(FinallizeFileResource finallizeFileResource, SecurityContext securityContext) {
        FileResource fileResource = finallizeFileResource.getFileResource();
        String md5 = fileResource.getMd5();
        if (!fileResource.getCreator().getId().equals(securityContext.getUser().getId())) {
            createSecurityLinkForFileResource(securityContext, fileResource);

        }
        File file = new File(fileResource.getFullPath());
        String actualMd5 = generateMD5(file);
        if (!md5.equals(actualMd5)) {
            file.delete();
            return new FinalizeFileResourceResponse().setExpectedMd5(md5).setMd5(actualMd5).setFileResource(fileResource).setFinalized(false);
        } else {
            fileResource.setDone(true);
        }
        return new FinalizeFileResourceResponse().setExpectedMd5(md5).setMd5(md5).setFileResource(fileResource).setFinalized(true);
    }

    public Job finalizeUpload(String md5, SecurityContext securityContext, String hint, Properties prop) {
        Job job;

        FileResource fileResource = getExistingFileResource(md5, securityContext);


        if (fileResource != null) {
            if (!fileResource.getCreator().getId().equals(securityContext.getUser().getId())) {
                createSecurityLinkForFileResource(securityContext, fileResource);

            }
            File file = new File(fileResource.getFullPath());
            String actualMd5 = generateMD5(file);
            if (!md5.equals(actualMd5)) {
                file.delete();
                throw new BadRequestException("file " + file.getAbsolutePath() + " failed md5 check , actual md5: " + actualMd5 + " expected: " + md5 + ", file has been deleted , please upload again");
            }
            fileResource.setDone(true);
            if (prop != null) {
                String type = prop.getProperty("fileType");
                if (type != null && !type.isEmpty()) {
                    fileResource.setType(new FileType(type));
                }


            }
            if (prop != null && Boolean.valueOf(prop.getProperty("dontProcess", "false"))) {
                return null;
            }

            job = new Job();
            JobInformation info = new JobInformation();
            info.setJobInfo(fileResource);
            info.setHandle(true); // tells the PI system to read the next
            // Cycle.
            info.setHandler(AnalyzerPlugin.class); // the first PI to run
            // (or multiple of) will// be an Analyzer PI
            info.setJobProperties(prop);
            job.setCurrentPhase(ProcessPhase.Waiting.getName());
            job.setCurrentPhasePrecentage(0);
            job.setJobInformation(info);
            job.setSecurityContext(securityContext); // We need to know who
            // has invoked it.
            jobService.putFileProcessJob(job); // Keeps the FC Job in a
            // static data structure to
            // get the Job from the
            // JobID that we will put
            // into Properties.
            JobOperator jo = BatchRuntime.getJobOperator();
            if (prop == null) {
                prop = new Properties();
            }

            prop.setProperty("fileProcessJobId", job.getId());// Associate
            // the FC
            // Job with
            // Java
            // Batch Job
            if (hint != null && !hint.isEmpty()) {
                prop.setProperty("hint", hint);
            }
            merge(fileResource);

            long jid = jo.start("genericJob", prop); // Starts Java Batch
            // JOB , genericJob
            // is the name and
            // ID of a Batch Job
            // defined in
            // Meta-inf/Batch-jobs
            job.setBatchJobId(jid);


        } else {
            // cannot happen!
            logger.severe("No file resource found for MD5:  " + md5);
            throw new ClientErrorException("the MD5 on finalize was not found in the database",
                    Response.Status.BAD_REQUEST);

        }
        return job;
    }

    @Override
    public void saveFile(InputStream is, FileResource file) {
        saveFile(is,null,file);
    }
    @Override
    public void saveFile(InputStream is,String chunkMd5, FileResource file) {
        try {
            byte[] data = IOUtils.toByteArray(is);
            if(chunkMd5!=null){
                String calculatedChunkMd5=generateMD5(new ByteArrayInputStream(data));
                if(!chunkMd5.equals(calculatedChunkMd5)){
                    throw new ClientErrorException("Chunk MD5 was "+calculatedChunkMd5 +" expected "+chunkMd5, Response.Status.PRECONDITION_FAILED);
                }
            }

            saveFile(data, file.getOffset(), file);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to read data from received input stream", e);
        }
    }


    @Override
    public boolean saveFile(byte[] data, long offsetInFile, FileResource file) {
        File f = new File(file.getFullPath());
        File parentFile = f.getParentFile();
        if (parentFile == null) {
            logger.log(Level.SEVERE, "unable to save file at " + file.getFullPath());
            return false;
        }
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                logger.warning("Failed Creating dir " + parentFile);
                return false;
            }
        }
        long written;

        try (RandomAccessFile fos = new RandomAccessFile(f, "rw")) {
            fos.seek(offsetInFile);
            fos.write(data);
            written = data.length;
            file.setOffset(offsetInFile + written);


        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to upload , truncating file to last known good location", e);
            long orig = file.getOffset();
            long fileOffset = trimToSize(f, orig);
            file.setOffset(fileOffset);


        } finally {
            fileResourceRepository.merge(file);
        }
        return true;

    }

    private long trimToSize(File file, long orig) {
        try (FileChannel fc = new FileOutputStream(file, true).getChannel()) {
            fc.truncate(orig);
            return orig;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed truncating file to orig", e);
        }
        return file.length();
    }


    @Override
    public FileResource create(String pathToFileResource, SecurityContext securityContext) {
        FileResource fileResource = null;
        File file = new File(pathToFileResource);
        if (file.exists()) {
            String s = file.isFile()?generateMD5(file):null;
            fileResource = fileResourceRepository.create(file.getName(), securityContext, s, pathToFileResource);

        }
        return fileResource;

    }

    @Override
    public void persist(Object o) {
        fileResourceRepository.persist(o);
    }

    @Override
    public FileResource createDontPersist(String pathToFileResource, SecurityContext securityContext) {
        File file=new File(pathToFileResource);
        String filename=file.getName();
        String ext = filename.endsWith("tar.gz") ? "tar.gz" : FilenameUtils.getExtension(filename);
        String md5 = generateMD5(pathToFileResource);

        FileResourceCreate fileResourceCreate = new FileResourceCreate()
                .setFullPath(pathToFileResource)
                .setMd5(md5)
                .setOffset(0L)
                .setActualFilename(UUID.randomUUID().toString() + "." + ext)
                .setUrl(md5!=null?Constants.UPLOAD_URL+md5:null)
                .setOriginalFilename(filename);
        return createNoMerge(fileResourceCreate,securityContext);
    }


    @Override
    public FileResource createNoMerge(FileResourceCreate fileResourceCreate, SecurityContext securityContext){
        FileResource fileResource=new FileResource(fileResourceCreate.getName(),securityContext);
        updateFileResourceNoMerge(fileResourceCreate,fileResource);
        return fileResource;
    }

    @Override
    public boolean updateFileResourceNoMerge(FileResourceCreate fileResourceCreate, FileResource fileResource) {
        boolean update=baseclassNewService.updateBaseclassNoMerge(fileResourceCreate,fileResource);
        if(fileResourceCreate.getMd5()!=null && !fileResourceCreate.getMd5().equals(fileResource.getMd5())){
            fileResource.setMd5(fileResourceCreate.getMd5());
            update=true;
        }

        if(fileResourceCreate.getFullPath()!=null && !fileResourceCreate.getFullPath().equals(fileResource.getFullPath())){
            fileResource.setFullPath(fileResourceCreate.getFullPath());
            update=true;
        }

        if(fileResourceCreate.getActualFilename()!=null && !fileResourceCreate.getActualFilename().equals(fileResource.getActualFilename())){
            fileResource.setActualFilename(fileResourceCreate.getActualFilename());
            update=true;
        }

        if(fileResourceCreate.getOriginalFilename()!=null && !fileResourceCreate.getOriginalFilename().equals(fileResource.getOriginalFilename())){
            fileResource.setOriginalFilename(fileResourceCreate.getOriginalFilename());
            update=true;
        }

        if(fileResourceCreate.getUrl()!=null && !fileResourceCreate.getUrl().equals(fileResource.getUrl())){
            fileResource.setUrl(fileResourceCreate.getUrl());
            update=true;
        }

        if(fileResourceCreate.getOffset()!=null && fileResourceCreate.getOffset()!=fileResource.getOffset()){
            fileResource.setOffset(fileResourceCreate.getOffset());
            update=true;
        }


        return update;

    }


    @Override
    @Deprecated
    public <T extends FileResource> T createDontPersist(Class<T> c, String pathToFileResource, SecurityContext securityContext) {
        T fileResource = null;
        File file = new File(pathToFileResource);
        if (file.exists()) {
            String s = generateMD5(file);
            fileResource = fileResourceRepository.createDontPersist(c, file.getName(), securityContext, s, pathToFileResource);

        }
        return fileResource;

    }


    @Override
    public String generateMD5(InputStream is) {
        String hash = null;
        try {

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[10240];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }

            byte[] hashed = digest.digest();
            hash = Hex.encodeHexString(hashed);
            is.close();

        } catch (NoSuchAlgorithmException | IOException e) {
            logger.log(Level.WARNING, "unable to generate MD5", e);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e1) {
                    logger.log(Level.WARNING, "unable to close is", e1);
                }
            }

        }
        return hash;
    }

    @Override
    public String generateMD5(String filePath) {
        if(filePath!=null){
            File file=new File(filePath);
            if(file.exists()){
                return generateMD5(file);
            }
        }
        return null;
    }


        @Override
        public String generateMD5(File file) {

        FileInputStream is;
        try {
            is = new FileInputStream(file);
            return generateMD5(is);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "could not open stream", e);
        }
        return null;

    }

    @Override
    public void merge(Object o) {
        fileResourceRepository.merge(o);
    }


    public FileType getFileType(String fileType, SecurityContext securityContext) {
        return fileResourceRepository.getFileType(fileType,
                new QueryInformationHolder<>(FileType.class, securityContext));

    }

    public List<FileType> getAllFileType(SecurityContext securityContext) {
        return fileResourceRepository.getAllFiltered(new QueryInformationHolder<>(FileType.class, securityContext));

    }

    public FileResource getFileResource(String id, SecurityContext securityContext) {
        return baselinkRepository.getByIdOrNull(id, FileResource.class, null, securityContext);
    }

    public FileResource getFileResourceUnsecure(String id) {
        return baselinkRepository.findByIdOrNull(FileResource.class, id);
    }

    @Override
    public PaginationResponse<FileResource> getAllFileResources(FileResourceFilter fileResourceFilter,SecurityContext securityContext){
        List<FileResource> fileResources=listAllFileResources(fileResourceFilter,securityContext);
        long count=fileResourceRepository.countAllFileResources(fileResourceFilter,securityContext);
        return new PaginationResponse<>(fileResources,fileResourceFilter,count);
    }

    @Override
    public List<FileResource> listAllFileResources(FileResourceFilter fileResourceFilter, SecurityContext securityContext) {
        return fileResourceRepository.listAllFileResources(fileResourceFilter,securityContext);
    }

    public void deleteFileResource(FileResource fr, User user, List<Tenant> tenant) {
        // TODO Auto-generated method stub

    }

    public List<FileResource> validate(SecurityContext securityContext) {
        List<FileResource> nonValidFiles = new ArrayList<>();
        List<FileResource> list = fileResourceRepository
                .getAllFiltered(new QueryInformationHolder<>(FileResource.class, securityContext));
        for (FileResource fileResource : list) {
            String path = fileResource.getFullPath();
            if (path != null && !path.isEmpty() && !new File(path).exists()) {
                nonValidFiles.add(fileResource);
            }

        }
        return nonValidFiles;
    }

    @Override
    public FileResource registerFile(String path, boolean calculateMd5, SecurityContext securityContext) throws FileNotFoundException {
        return registerFile(path, -1, calculateMd5, securityContext);
    }


    public FileResource registerFile(String path, long dateTaken, boolean calculateMd5, SecurityContext securityContext) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("file at: " + path + "was not found");
        } else {
            String name = file.getName();
            String md5 = null;
            if (calculateMd5) {
                md5 = generateMD5(file);
            }
            FileResource fileResource = null;
            if (md5 != null && !md5.isEmpty()) {
                fileResource = getExistingFileResource(md5, securityContext);

            }


            if (fileResource == null) {
                fileResource = new FileResource("name", securityContext);
            } else {
                if (!fileResource.getCreator().getId().equals(securityContext.getUser().getId())) {
                    createSecurityLinkForFileResource(securityContext, fileResource);
                }

            }

            fileResource.setName(name);
            fileResource.setActualFilename(name);
            fileResource.setFullPath(path);
            fileResource.setDone(true);
            fileResource.setMd5(md5);
            if (dateTaken > 0) {

                fileResource.setDateTaken(OffsetDateTime.ofInstant(Instant.ofEpochMilli(dateTaken), ZoneId.of("UTC")));
            }

            fileResourceRepository.merge(fileResource);


            return fileResource;

        }

    }

    public List<FileResource> listOfType(FilteringInformationHolder filteringInformationHolder, int pagesize, int currentPage, OffsetDateTime start, String type, SecurityContext securityContext) {
        return fileResourceRepository.listOfType(filteringInformationHolder, pagesize, currentPage, start, type, securityContext);
    }

    @Override
    public void massMerge(List<?> resources) {
        fileResourceRepository.massMerge(resources);
    }

    @Override
    public void refrehEntityManager() {

        baselinkRepository.refrehEntityManager();
        fileResourceRepository.refrehEntityManager();
    }

    @Override
    public void validate(FinallizeFileResource finallizeFileResource, SecurityContext securityContext) {
        FileResource fileResource = finallizeFileResource.getFileResourceId() != null ? baselinkRepository.getByIdOrNull(finallizeFileResource.getFileResourceId(), FileResource.class, null, securityContext) : null;
        if (fileResource == null) {
            throw new BadRequestException("No FileResource with id " + finallizeFileResource.getFileResourceId());
        }
        finallizeFileResource.setFileResource(fileResource);

    }

    @Override
    public void validate(ZipAndDownloadRequest zipAndDownloadRequest, SecurityContext securityContext) {
        Set<String> fileResourceIds = zipAndDownloadRequest.getFileResourceIds();
        Map<String, FileResource> fileResourceMap = fileResourceIds.isEmpty() ? new HashMap<>() : fileResourceRepository.listByIds(FileResource.class, fileResourceIds, securityContext).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        fileResourceIds.removeAll(fileResourceMap.keySet());
        if (!fileResourceIds.isEmpty()) {
            String message = "No FileResources With ids " + fileResourceIds;
            if (zipAndDownloadRequest.isFailOnMissing() || fileResourceMap.isEmpty()) {
                throw new BadRequestException(message);
            } else {
                logger.warning(message);
            }
        }
        zipAndDownloadRequest.setFileResources(new ArrayList<>(fileResourceMap.values()));

    }

    @Override
    public byte[] readFilePart(File file, long offset) {
        byte[] data = new byte[MAX_FILE_PART_SIZE];
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.seek(offset);
            int read = randomAccessFile.read(data, 0, MAX_FILE_PART_SIZE);
            if (read < MAX_FILE_PART_SIZE) {
                data = Arrays.copyOf(data, read);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "unable to read file part", e);
        }
        return data;
    }

    @Override
    public List<ZipFile> listAllZipFiles(ZipFileFilter zipFileFilter, SecurityContext securityContext) {
        return fileResourceRepository.listAllZipFiles(zipFileFilter, securityContext);
    }


    @Override
    public ZipFile zipAndDownload(ZipAndDownloadRequest zipAndDownload, SecurityContext securityContext) {

        ZipFile existing = getExistingZipFile(zipAndDownload.getFileResources(), securityContext);
        if (existing == null) {
            List<Object> toMerge = new ArrayList<>();
            List<File> files = zipAndDownload.getFileResources().parallelStream().map(f -> new File(f.getFullPath())).collect(Collectors.toList());
            File[] arr = new File[files.size()];
            files.toArray(arr);
            File zip = new File(com.flexicore.service.FileResourceService.generateNewPathForFileResource("zip", securityContext.getUser()) + ".zip");
            ZipUtil.packEntries(arr, zip);
            ZipFile zipFile = new ZipFile(zip.getName(), securityContext);
            zipFile.setOriginalFilename(zip.getName());
            zipFile.setFullPath(zip.getAbsolutePath());
            zipFile.setMd5(generateMD5(zip));
            zipFile.setOffset(zip.length());
            toMerge.add(zipFile);
            List<ZipFileToFileResource> links = zipAndDownload.getFileResources().parallelStream().map(f -> createZipFileToFileResourceNoMerge(zipFile, f, securityContext)).collect(Collectors.toList());
            toMerge.addAll(links);
            fileResourceRepository.massMerge(toMerge);
            existing = zipFile;

        }
        return existing;

    }

    private ZipFile getExistingZipFile(List<FileResource> fileResources, SecurityContext securityContext) {
        Set<String> requiredIds = fileResources.parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
        List<ZipFileToFileResource> links = fileResourceRepository.listZipFileToFileResource(new ZipFileToFileResourceFilter().setFileResources(fileResources), securityContext);
        Map<String, ZipFile> zipFileMap = links.parallelStream().map(f -> f.getZipFile()).filter(f -> f.getFullPath() != null && new File(f.getFullPath()).exists()).collect(Collectors.toMap(f -> f.getId(), f -> f, (a, b) -> a));
        Map<String, Set<String>> zipFileToFileResource = links.parallelStream().collect(Collectors.groupingBy(f -> f.getZipFile().getId(), Collectors.mapping(f -> f.getZippedFile().getId(), Collectors.toSet())));
        for (Map.Entry<String, Set<String>> stringSetEntry : zipFileToFileResource.entrySet()) {
            if (stringSetEntry.getValue().containsAll(requiredIds) && requiredIds.containsAll(stringSetEntry.getValue())) {
                return zipFileMap.get(stringSetEntry.getKey());
            }
        }
        return null;
    }

    private ZipFileToFileResource createZipFileToFileResourceNoMerge(ZipFile zipFile, FileResource f, SecurityContext securityContext) {
        ZipFileToFileResource zipFileToFileResource = new ZipFileToFileResource("link", securityContext);
        zipFileToFileResource.setZipFile(zipFile);
        zipFileToFileResource.setZippedFile(f);
        return zipFileToFileResource;
    }

    public Response download( long offset,long size,  String id, String remoteIp,  SecurityContext securityContext) {
        FileResource fileResource = getFileResource(id, securityContext);
        if (fileResource == null) {
            throw new BadRequestException("No File resource with id " + id);
        }
        if (fileResource.isNonDownloadable()) {
            throw new ClientErrorException("file resource with id: " + id +
                    "is not available for download", Response.Status.BAD_REQUEST);
        }
        if (fileResource.getOnlyFrom() != null) {
            Set<String> allowedIps = Stream.of(fileResource.getOnlyFrom().split(",")).collect(Collectors.toSet());
            if(!allowedIps.contains(remoteIp)){
                throw new BadRequestException("File is not allowed to be downloaded from "+remoteIp);
            }


        }

        return prepareFileResourceForDownload(fileResource, offset, size);
    }

    public Response prepareFileResourceForDownload(FileResource fileResource, long offset, long size) {
        if (fileResource != null) {
            MimetypesFileTypeMap map = new MimetypesFileTypeMap();
            File file = new File(fileResource.getFullPath());
            String mimeType = map.getContentType(file);
            String name = fileResource.getOriginalFilename();
            if (name == null) {
                name = file.getName();
            }
            if (file.exists()) {
                long fileLength = file.length();
                if (offset >= fileLength) {
                    throw new BadRequestException("received offset(" + offset + ") >= length(" + fileLength);
                }

                try {
                    InputStream inputStream = new FileInputStream(file);
                    inputStream.skip(offset);
                    if (size > 0) {
                        inputStream = new BoundedInputStream(inputStream, size);
                    }
                    Response.ResponseBuilder response = Response.ok(inputStream, mimeType);
                    int available = inputStream.available();
                    long contentLength = size > 0 ? Math.min(available, size) : available;
                    response.header("Content-Length", contentLength);
                    response.header("Content-Disposition", "attachment; filename=\"" + name + "\"");
                    response.header("fileName", name);

                    return response.build();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "failed opening file", e);
                }

            }
        }
        throw new ClientErrorException(HttpResponseCodes.SC_BAD_REQUEST);
    }
}
