package com.example.saas.dm;

import com.example.saas.dm.dto.DmChannelReplyPayload;
import com.example.saas.dm.dto.DmChannelSendResult;

public interface DmChannelSender {
    boolean supports(String channel);
    DmChannelSendResult send(DmChannelReplyPayload payload);
}
