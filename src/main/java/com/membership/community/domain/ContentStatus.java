package com.membership.community.domain;

/** Estado de moderación de un contenido (post o comentario). */
public enum ContentStatus {
    VISIBLE,   // publicado y visible
    FLAGGED,   // marcado automáticamente, pendiente de revisión
    HIDDEN     // oculto por moderación
}
