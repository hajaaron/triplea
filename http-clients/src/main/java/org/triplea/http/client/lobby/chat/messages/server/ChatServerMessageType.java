package org.triplea.http.client.lobby.chat.messages.server;

import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Chat message types that a server can send over websocket to client. */
@AllArgsConstructor
@SuppressWarnings("ImmutableEnumChecker")
@Getter(onMethod_ = @Override)
public enum ChatServerMessageType implements WebsocketMessageType<ChatMessageListeners> {
  CHAT_EVENT(String.class, ChatMessageListeners::getChatEventListener),
  CHAT_MESSAGE(ChatMessage.class, ChatMessageListeners::getChatMessageListener),
  PLAYER_JOINED(ChatParticipant.class, ChatMessageListeners::getPlayerJoinedListener),
  PLAYER_LEFT(PlayerName.class, ChatMessageListeners::getPlayerLeftListener),
  PLAYER_LISTING(ChatterList.class, ChatMessageListeners::getConnectedListener),
  PLAYER_SLAPPED(PlayerSlapped.class, ChatMessageListeners::getPlayerSlappedListener),
  SERVER_ERROR(String.class, ChatMessageListeners::getServerErrorListener),
  STATUS_CHANGED(StatusUpdate.class, ChatMessageListeners::getPlayerStatusListener);

  private final Class<?> classType;
  private final Function<ChatMessageListeners, Consumer<?>> listenerMethod;
}