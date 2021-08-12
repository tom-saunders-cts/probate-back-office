package uk.gov.hmcts.probate.service.documentmanagement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.probate.model.DocumentType;
import uk.gov.hmcts.probate.model.evidencemanagement.EvidenceManagementFileUpload;
import uk.gov.hmcts.probate.service.evidencemanagement.builder.ByteArrayMultipartFile;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class DocumentManagementRequestBuilder {

    private static final String CLASSIFICATION_PRIVATE_PARAMETER = "PRIVATE";
    private static final String JURISDICTION = "PROBATE";

    public DocumentUploadRequest perpareDocumentUploadRequest(EvidenceManagementFileUpload file,
                                                                     DocumentType documentType) {
        MultipartFile multipartFile = ByteArrayMultipartFile.builder()
            .content(file.getBytes())
            .contentType(file.getContentType())
            .name(file.getFileName())
            .build();

        List<MultipartFile> multipartFileList = Arrays.asList(multipartFile);
        return new DocumentUploadRequest(CLASSIFICATION_PRIVATE_PARAMETER,
            documentType.getCcdCaseType(), JURISDICTION, multipartFileList);

    }
}
