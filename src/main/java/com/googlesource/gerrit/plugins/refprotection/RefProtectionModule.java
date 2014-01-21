package com.googlesource.gerrit.plugins.refprotection;

import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;

public class RefProtectionModule extends AbstractModule {
  @Override
  protected void configure() {
    DynamicSet.bind(binder(), GitReferenceUpdatedListener.class).to(
        RefUpdateListener.class);
  }
}
