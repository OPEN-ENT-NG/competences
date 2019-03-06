package fr.openent.competences.service.impl;

import java.util.concurrent.atomic.AtomicBoolean;

public interface LSUService {

    void serviceResponseOK (AtomicBoolean answer, int count, String thread, String method);
}
