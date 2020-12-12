package com.flexicore.service;

import com.flexicore.data.UserRepository;
import com.flexicore.model.User;
import com.flexicore.request.*;
import com.flexicore.response.FinishTotpSetupResponse;
import com.flexicore.response.SetupTotpResponse;
import com.flexicore.response.TotpAuthenticationResponse;
import com.flexicore.security.SecurityContext;
import com.flexicore.service.impl.EncryptionService;
import com.lambdaworks.crypto.SCryptUtil;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Component
public class TotpService {

    private static final Logger logger = LoggerFactory.getLogger(TotpService.class);

    @Value("${flexicore.security.totp.appName:FlexiCore}")
    private String totpAppName;
    @Value("${flexicore.security.totp.recovery.scryptN:16384}")
    private int scryptN;
    @Value("${flexicore.security.totp.recovery.scryptR:8}")
    private int scryptR;
    @Value("${flexicore.security.totp.recovery.scryptP:1}")
    private int scryptP;
    @Autowired
    private SecretGenerator secretGenerator;

    @Autowired
    private QrDataFactory qrDataFactory;

    @Autowired
    private QrGenerator qrGenerator;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private UserRepository userrepository;

    @Autowired
    private CodeVerifier verifier;

    @Autowired
    private TokenService tokenService;
    @Autowired
    private RecoveryCodeGenerator recoveryCodeGenerator;

    public SetupTotpResponse setupTotp(StartTotpSetup startTotpSetup) {
        User user = startTotpSetup.getUser();
        if (user.isTotpEnabled()) {
            throw new BadRequestException("Totp already enabled for " + user.getEmail() + "(" + user.getId() + ")");
        }
        SetupTotpResponse setupTotpResponse = new SetupTotpResponse();
        try {
            String secret = secretGenerator.generate();
            String encrypt = Base64.getEncoder().encodeToString(encryptionService.encrypt(secret.getBytes(), user.getId().getBytes()));
            user.setTotpSecret(encrypt);
            userrepository.merge(user);
            setupTotpResponse.setSecret(secret);

            QrData data = qrDataFactory.newBuilder()
                    .label(user.getEmail())
                    .secret(secret)
                    .issuer(totpAppName)
                    .build();

            // Generate the QR code image data as a base64 string which
            // can be used in an <img> tag:
            String qrCodeImage = getDataUriForImage(
                    qrGenerator.generate(data),
                    qrGenerator.getImageMimeType()
            );
            setupTotpResponse.setBase64QRCode(qrCodeImage);

        } catch (Exception e) {
            logger.error("failed setup totp", e);
        }
        return setupTotpResponse;
    }

    public void validate(TotpAuthenticationRequest totpAuthenticationRequest, SecurityContext securityContext) {
        String code = totpAuthenticationRequest.getCode();
        if (code == null) {
            throw new BadRequestException("totp code cannot be null");
        }

        User user = securityContext.getUser();
        if (!user.isTotpEnabled()) {
            throw new BadRequestException("user does not have totp enabled");
        }
        if (user.getTotpSecret() == null) {
            throw new BadRequestException("user does not have a valid secret key");
        }

    }


    public void validate(FinishTotpSetupRequest finishSetupTotp, SecurityContext securityContext) {
        if (finishSetupTotp.getUser() == null) {
            finishSetupTotp.setUser(securityContext.getUser());
        }
        User user = finishSetupTotp.getUser();
        String totpSecret = user.getTotpSecret();
        if (totpSecret == null) {
            throw new BadRequestException("user did not start totp setup");
        }
        if (user.isTotpEnabled()) {
            throw new BadRequestException("user already finished totp setup");
        }
        try {
            byte[] salt = user.getId().getBytes();
            String secret = getDecryptedSecret(totpSecret, salt);
            if (!secret.equals(finishSetupTotp.getSecret())) {
                throw new BadRequestException("given secret does not match the one setup");
            }
        } catch (GeneralSecurityException e) {
            logger.error("failed decrypting totp", e);
            throw new InternalServerErrorException("failed decrypting totp");
        }


    }

    private String getDecryptedSecret(String encryptedSecret, byte[] salt) throws GeneralSecurityException {
        return new String(encryptionService.decrypt(Base64.getDecoder().decode(encryptedSecret), salt));
    }

    public FinishTotpSetupResponse finishSetupTotp(FinishTotpSetupRequest finishSetupTotp, SecurityContext securityContext) {
        User user = finishSetupTotp.getUser();
        user.setTotpEnabled(true);
        List<String> totpCodes = Stream.of(recoveryCodeGenerator.generateCodes(8)).collect(Collectors.toList());
        String codes = totpCodes.stream().map(f -> hashRecoveryCode(f)).collect(Collectors.joining("|"));
        user.setTotpRecoveryCodes(codes);
        userrepository.merge(user);
        return new FinishTotpSetupResponse().setTotpRecoveryCodes(totpCodes);
    }



    private String hashRecoveryCode(String plain) {
        return SCryptUtil.scrypt(plain, scryptN, scryptR, scryptP);
    }

    public TotpAuthenticationResponse authenticateTotp(TotpAuthenticationRequest totpAuthenticationRequest, SecurityContext securityContext) {
        User user = securityContext.getUser();
        try {
            String secret = getDecryptedSecret(user.getTotpSecret(), user.getId().getBytes());
            if (verifier.isValidCode(secret, totpAuthenticationRequest.getCode())) {
                return getTotpAuthenticationResponse(securityContext, user);

            }
            throw new BadRequestException("Totp code did not match");
        } catch (GeneralSecurityException e) {
            logger.error("failed decrypting totp", e);
            throw new InternalServerErrorException("failed decrypting totp");
        }
    }

    private TotpAuthenticationResponse getTotpAuthenticationResponse(SecurityContext securityContext, User user) {
        String writeTenant = securityContext.getTenantToCreateIn() != null ? securityContext.getTenantToCreateIn().getId() : null;
        Set<String> readTenants = securityContext.getTenants() != null && !securityContext.getTenants().isEmpty() ? securityContext.getTenants().stream().map(f -> f.getId()).collect(Collectors.toSet()) : null;
        OffsetDateTime expirationDate = securityContext.getExpiresDate();
        String authenticationToken = tokenService.getJwtToken(user, expirationDate, writeTenant, readTenants, true);
        return new TotpAuthenticationResponse().setTotpAuthenticationToken(authenticationToken);
    }

    public void validate(DisableTotpRequest disableTotpRequest, SecurityContext securityContext) {
        if(disableTotpRequest.getUser()==null){
            disableTotpRequest.setUser(securityContext.getUser());
        }
        if(!disableTotpRequest.getUser().isTotpEnabled()){
            throw new BadRequestException("totp is not enabled for user");
        }

    }

    public void disableTotp(DisableTotpRequest disableTotpRequest, SecurityContext securityContext) {
        User user = disableTotpRequest.getUser();
        user.setTotpSecret(null);
        user.setTotpRecoveryCodes(null);
        user.setTotpEnabled(false);
        userrepository.merge(user);
    }

    public void validate(RecoverTotpRequest recoverTotpRequest, SecurityContext securityContext) {
        User user=securityContext.getUser();
        if(!user.isTotpEnabled()){
            throw new BadRequestException("User does not have totp enabled");
        }
        if(user.getTotpRecoveryCodes()==null){
            throw new BadRequestException("user has no recovery codes");
        }

    }

    public TotpAuthenticationResponse recoverTotp(RecoverTotpRequest recoverTotpRequest, SecurityContext securityContext) {
        String code=recoverTotpRequest.getRecoveryCode();
        User user = securityContext.getUser();
        Set<String> totpCodes = Stream.of(user.getTotpRecoveryCodes().split("\\|")).collect(Collectors.toSet());
        Optional<String> hashedCode= totpCodes.stream().filter(f->SCryptUtil.check(code,f)).findFirst();
        if(hashedCode.isPresent()){
            totpCodes.remove(hashedCode.get());
            String unusedCodes= String.join("|", totpCodes);
            user.setTotpRecoveryCodes(unusedCodes);
            userrepository.merge(user);
            return getTotpAuthenticationResponse(securityContext,user);
        }
        throw new NotAuthorizedException("recovery code is invalid");
    }
}
