package cl.goldenoddjobs.bff.messaging;

import cl.goldenoddjobs.bff.dto.InformeEvent;

public interface InformePublisher {

    void publicar(InformeEvent evento);

    String proveedor();
}
