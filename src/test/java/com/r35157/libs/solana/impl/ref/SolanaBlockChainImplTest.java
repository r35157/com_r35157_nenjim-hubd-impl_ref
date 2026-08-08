package com.r35157.libs.solana.impl.ref;

import com.r35157.assetaz.services.cis.CurrencyIdentityService;
import com.r35157.assetaz.services.cis.ExternalCurrencyReference;
import com.r35157.assetaz.valuetypes.CurrencyType;
import com.r35157.libs.solana.SolanaLatestBlockhash;
import com.r35157.libs.solana.SolanaUnsignedTransaction;
import com.r35157.libs.valuetypes.basic.MoneyAmount;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.r35157.assetaz.services.cis.CurrencyTypeIds.SOLANA_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolanaBlockChainImplTest {
    @Test
    void buildsExpectedLegacySolTransferTransaction() throws Exception {
        SolanaBlockChainImpl blockChain = createBlockChain(
                0,
                0,
                new AtomicReference<>()
        );

        SolanaUnsignedTransaction transaction =
                blockChain.buildSolanaTransferTransaction(
                        SENDER,
                        RECIPIENT,
                        new MoneyAmount(
                                new BigDecimal("0.123456789"),
                                SOLANA
                        )
                );

        assertEquals(EXPECTED_TRANSACTION, transaction.serializedTransaction());
        assertEquals(BLOCKHASH, transaction.blockhash());
        assertEquals(
                LAST_VALID_BLOCK_HEIGHT,
                transaction.lastValidBlockHeight()
        );
    }

    @Test
    void transferAllSubtractsExactFeeFromBalance() throws Exception {
        AtomicReference<String> feeMessage = new AtomicReference<>();
        SolanaBlockChainImpl blockChain = createBlockChain(
                10_000,
                5_000,
                feeMessage
        );

        SolanaUnsignedTransaction transaction =
                blockChain.buildSolanaTransferAllTransaction(
                        SENDER,
                        RECIPIENT
                );

        byte[] transactionBytes = Base64.getDecoder().decode(
                transaction.serializedTransaction()
        );
        long transferredLamports = ByteBuffer
                .wrap(
                        transactionBytes,
                        transactionBytes.length - Long.BYTES,
                        Long.BYTES
                )
                .order(ByteOrder.LITTLE_ENDIAN)
                .getLong();

        assertEquals(5_000, transferredLamports);

        byte[] feeMessageBytes = Base64.getDecoder().decode(
                feeMessage.get()
        );
        byte[] finalMessageBytes = Arrays.copyOfRange(
                transactionBytes,
                1 + 64,
                transactionBytes.length
        );

        assertArrayEquals(
                Arrays.copyOf(
                        feeMessageBytes,
                        feeMessageBytes.length - Long.BYTES
                ),
                Arrays.copyOf(
                        finalMessageBytes,
                        finalMessageBytes.length - Long.BYTES
                )
        );
        assertEquals(
                10_000,
                readLastLongLittleEndian(feeMessageBytes)
        );
    }

    @Test
    void transferAllRejectsBalanceThatOnlyCoversFee() {
        SolanaBlockChainImpl blockChain = createBlockChain(
                5_000,
                5_000,
                new AtomicReference<>()
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> blockChain.buildSolanaTransferAllTransaction(
                        SENDER,
                        RECIPIENT
                )
        );

        assertEquals(
                "Wallet balance of 5000 lamports cannot cover the "
                        + "transaction fee of 5000 lamports and a positive "
                        + "transfer amount",
                exception.getMessage()
        );
    }

    @Test
    void rejectsAmountsSmallerThanOneLamport() {
        SolanaBlockChainImpl blockChain = createBlockChain(
                0,
                0,
                new AtomicReference<>()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> blockChain.buildSolanaTransferTransaction(
                        SENDER,
                        RECIPIENT,
                        new MoneyAmount(
                                new BigDecimal("0.0000000001"),
                                SOLANA
                        )
                )
        );
    }

    private static SolanaBlockChainImpl createBlockChain(
            long balance,
            long fee,
            AtomicReference<String> feeMessage
    ) {
        return new SolanaBlockChainImpl(CURRENCY_IDENTITIES) {
            @Override
            public long getBalanceInLamport(String address) {
                return balance;
            }

            @Override
            public SolanaLatestBlockhash getLatestBlockhash() {
                return new SolanaLatestBlockhash(
                        BLOCKHASH,
                        LAST_VALID_BLOCK_HEIGHT
                );
            }

            @Override
            public long getFeeForMessage(String serializedMessage) {
                feeMessage.set(serializedMessage);
                return fee;
            }
        };
    }

    private static long readLastLongLittleEndian(byte[] bytes) {
        return ByteBuffer
                .wrap(
                        bytes,
                        bytes.length - Long.BYTES,
                        Long.BYTES
                )
                .order(ByteOrder.LITTLE_ENDIAN)
                .getLong();
    }

    private static final CurrencyType SOLANA = new CurrencyType(
            SOLANA_ID,
            "Solana",
            "SOL"
    );
    private static final CurrencyIdentityService CURRENCY_IDENTITIES =
            new CurrencyIdentityService() {
                @Override
                public @NotNull CurrencyType resolve(@NotNull UUID currencyTypeId) {
                    if (!SOLANA_ID.equals(currencyTypeId)) {
                        throw new IllegalArgumentException("Unknown test currency: " + currencyTypeId);
                    }
                    return SOLANA;
                }

                @Override
                public @NotNull CurrencyType resolve(@NotNull ExternalCurrencyReference externalReference) {
                    throw new IllegalArgumentException(
                            "No external currencies configured for this test"
                    );
                }

                @Override
                public @NotNull Set<ExternalCurrencyReference> findExternalReferences(@NotNull UUID currencyTypeId) {
                    resolve(currencyTypeId);
                    return Set.of();
                }
            };
    private static final String SENDER =
            "So11111111111111111111111111111111111111112";
    private static final String RECIPIENT =
            "SysvarRent111111111111111111111111111111111";
    private static final String BLOCKHASH =
            "11111111111111111111111111111111";
    private static final long LAST_VALID_BLOCK_HEIGHT = 123_456;
    private static final String EXPECTED_TRANSACTION =
            "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAABAAEDBpuIV/6rgYT7aH9jRhjANdrEOdwa6ztVmKDw"
                    + "AAAAAAEGp9UXGSxcUSGMyUw9SvF/WNruCJuh/UTj29mKAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    + "AAAAAAABAgIAAQwCAAAAFc1bBwAAAAA=";
}
