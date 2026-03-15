package com.example.saas.dm;

import com.example.saas.dm.dto.DmChannelReplyPayload;
import com.example.saas.dm.dto.DmChannelSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FallbackDmChannelSender implements DmChannelSender {

    @Override
    public boolean supports(String channel) {
        return true;
    }

    @Override
    public DmChannelSendResult send(DmChannelReplyPayload payload) {
        log.warn("Unsupported DM channel fallback send: channel={}, senderChannelId={}, text={}",
                payload.channel(),
                payload.senderChannelId(),
                payload.text());
        return new DmChannelSendResult(false, "UNSUPPORTED_CHANNEL");
    }
}
