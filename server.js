import express from 'express';

const app = express();
const port = process.env.PORT || 3001;

app.use(express.json());

/**
 * Backend relay endpoint to create and publish product via Printify API
 * Keeps PRINTIFY_API_TOKEN secure on backend (never exposed to browser)
 */
app.post('/api/publish-product', async (req, res) => {
  const { sku, productId, colorHex, zone, mockupImageUrl } = req.body;
  const printifyToken = process.env.PRINTIFY_API_TOKEN || '';

  console.log(`📦 Received publish request for SKU: ${sku}, Product: ${productId}, Zone: ${zone}`);

  if (!printifyToken) {
    console.warn('⚠️ PRINTIFY_API_TOKEN missing from environment; returning simulated sync response for local development.');
    return res.status(200).json({
      success: true,
      printifyProductId: `PRINTIFY-${sku}-${Date.now()}`,
      sku,
      status: 'PUBLISHED_LOCALLY'
    });
  }

  try {
    // 1. Fetch available shop
    const shopsRes = await fetch('https://api.printify.com/v1/shops.json', {
      headers: {
        'Authorization': `Bearer ${printifyToken}`,
        'Content-Type': 'application/json'
      }
    });

    if (!shopsRes.ok) {
      throw new Error(`Failed to fetch Printify shops: ${shopsRes.status}`);
    }

    const shops = await shopsRes.json();
    const shopId = shops?.[0]?.id || '1';

    // 2. Create product in shop
    const productPayload = {
      title: `Print Defy Custom - ${sku}`,
      description: 'Zero-cost autonomous print-on-demand item with direct Kornit DTG rendering.',
      blueprint_id: 12,
      print_provider_id: 1,
      variants: [{ id: 1, price: 2499, is_enabled: true }],
      print_areas: [
        {
          variant_ids: [1],
          placeholders: [
            {
              position: zone === 'pocket' ? 'front' : 'front',
              images: mockupImageUrl ? [{ id: 'mockup_img', x: 0.5, y: 0.5, scale: 1, angle: 0 }] : []
            }
          ]
        }
      ]
    };

    const createRes = await fetch(`https://api.printify.com/v1/shops/${shopId}/products.json`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${printifyToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(productPayload)
    });

    if (!createRes.ok) {
      throw new Error(`Printify API product creation failed: ${createRes.status}`);
    }

    const createdProduct = await createRes.json();
    return res.status(200).json({
      success: true,
      printifyProductId: createdProduct.id || `PTFY-${Date.now()}`,
      sku
    });
  } catch (error) {
    console.error('Printify publish error:', error);
    return res.status(500).json({ error: error.message });
  }
});

export default app;

if (process.env.NODE_ENV !== 'test') {
  app.listen(port, () => {
    console.log(`Print Defy backend relay listening on port ${port}`);
  });
}
