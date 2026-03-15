package com.example.saas.dm;

import com.example.saas.dm.dto.DmChannelReplyPayload;
import com.example.saas.dm.dto.DmChannelSendResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutingDmChannelSender implements DmChannelSender {

    private final List<DmChannelSender> senders;

    public RoutingDmChannelSender(List<DmChannelSender> senders) {
        this.senders = senders.stream()
                .filter(sender -> !(sender instanceof RoutingDmChannelSender))
                .toList();
    }

    @Override
    public boolean supports(String channel) {
        return true;
    }

    @Override
    public DmChannelSendResult send(DmChannelReplyPayload payload) {
        return senders.stream()
                .filter(sender -> !(sender instanceof FallbackDmChannelSender))
                .filter(sender -> sender.supports(payload.channel()))
                .findFirst()
                .orElseGet(() -> senders.stream()
                        .filter(sender -> sender instanceof FallbackDmChannelSender)
                        .findFirst()
                        .orElseThrow())
                .send(payload);
    }
}
