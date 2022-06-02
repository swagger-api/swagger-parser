package io.swagger.v3.parser.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

public class DereferencersFactory {

    private static DereferencersFactory instance;
    static Logger LOGGER = LoggerFactory.getLogger(DereferencersFactory.class);
    private final List<OpenAPIDereferencer> dereferencers;

    private DereferencersFactory() {
        dereferencers = new CopyOnWriteArrayList<>();
        dereferencers.add(new OpenAPIDereferencer31());
    }

    public static DereferencersFactory getInstance() {
        if (instance == null) {
            instance = new DereferencersFactory();
        }
        return instance;
    }

    public void addDereferencer(OpenAPIDereferencer dereferencer) {
        dereferencers.add(0, dereferencer);
    }

    public void removeDereferencer(OpenAPIDereferencer dereferencer) {
        dereferencers.remove(dereferencer);
    }

    public List<OpenAPIDereferencer> getDereferencers() {
        return Collections.unmodifiableList(dereferencers);
    }

    static {
        ServiceLoader<OpenAPIDereferencer> loader = ServiceLoader.load(OpenAPIDereferencer.class);
        Iterator<OpenAPIDereferencer> itr = loader.iterator();
        while (itr.hasNext()) {
            OpenAPIDereferencer ext = itr.next();
            if (ext == null) {
                LOGGER.error("failed to load extension {}", ext);
            } else {
                instance.addDereferencer(ext);
                LOGGER.debug("adding OpenAPIDereferencer: {}", ext);
            }
        }
    }
}
