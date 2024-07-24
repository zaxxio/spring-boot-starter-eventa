package org.eventa.core.dispatcher;

public interface QueryDispatcher {
    <Q, R> R dispatch(Q query, ResponseType<R> responseType);
}
