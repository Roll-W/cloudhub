package org.huel.cloudhub.client.data.dto;

/**
 * @author RollW
 */
public interface VerifiableToken {
    String token();

    Long userId();

    Long expiryDate();

    boolean used();
}
