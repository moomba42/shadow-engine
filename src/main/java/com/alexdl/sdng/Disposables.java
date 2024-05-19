package com.alexdl.sdng;

import com.alexdl.sdng.backend.Disposable;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class Disposables implements Disposable {
    private final Set<Disposable> disposables;

    @Inject
    public Disposables() {
        disposables = new HashSet<>();
    }

    public void add(Disposable disposable) {
        disposables.add(disposable);
    }

    @Override
    public void dispose() {
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
    }
}
