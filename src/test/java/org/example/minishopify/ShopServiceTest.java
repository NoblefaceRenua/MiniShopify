package org.example.minishopify;

import org.example.minishopify.model.Shop;
import org.example.minishopify.repository.ShopRepository;
import org.example.minishopify.service.ShopService;
import org.example.minishopify.web.ShopForm;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopServiceTest {
    @Test
    void trimsShopDetailsAndNormalizesTags() {
        ShopRepository repository = mock(ShopRepository.class);
        ShopForm form = new ShopForm();
        form.setName(" Store ");
        form.setDescription(" Description ");
        form.setTags(" Handmade, clothes, HANDMADE, ");
        new ShopService(repository).createShop(form);
        ArgumentCaptor<Shop> captured = ArgumentCaptor.forClass(Shop.class);
        verify(repository).save(captured.capture());
        assertEquals("Store", captured.getValue().getName());
        assertEquals("Description", captured.getValue().getDescription());
        assertEquals("handmade, clothes", captured.getValue().getTags());
    }
}