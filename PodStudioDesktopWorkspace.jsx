import React, { useState, useEffect, useRef } from 'react';
import { fabric } from 'fabric';

// Section 3D — Stripe Buy Button embed for Printify POD Pop-Up physical storefront
export const StripeBuyButton = ({ buyButtonId }) => (
  <stripe-buy-button
    buy-button-id={buyButtonId}
    publishable-key={process.env.REACT_APP_STRIPE_PUBLISHABLE_KEY}
  />
);

// Print zone geometry map (500x600 canvas space)
const PRINT_ZONE_BOUNDS = {
  front: { left: 130, top: 120, width: 240, height: 300 },
  back: { left: 130, top: 110, width: 240, height: 310 },
  'left-chest': { left: 275, top: 145, width: 85, height: 85 },
  'right-chest': { left: 140, top: 145, width: 85, height: 85 },
  'left-sleeve': { left: 180, top: 170, width: 140, height: 180 },
  'right-sleeve': { left: 180, top: 170, width: 140, height: 180 },
  hood: { left: 175, top: 70, width: 150, height: 140 },
  hem: { left: 120, top: 410, width: 260, height: 90 }
};

/**
 * PodStudioDesktopWorkspace — Print Defy POD Pop-Up Studio (ULTRA Single-Agent Architecture)
 * 3-Panel Printify Studio Clone Canvas · Live Printify Catalog · Zero Fake Data
 */
export function PodStudioDesktopWorkspace({ onNavigateAway }) {
  // Section 4B — Frontend catalog browser state (live Printify API — no hardcoded products)
  const [catalog, setCatalog] = useState([]); // All blueprints
  const [catalogCategory, setCatalogCategory] = useState('all'); // Filter
  const [selectedBlueprint, setSelectedBlueprint] = useState(null); // Chosen product type
  const [blueprintVariants, setBlueprintVariants] = useState([]); // Sizes/colors for chosen product
  const [selectedVariant, setSelectedVariant] = useState(null); // Chosen size+color combo
  const [selectedProviderId, setSelectedProviderId] = useState(null);

  // Section 4C & 4D — Canvas, View, Print Zone & Layer state
  const [canvasView, setCanvasView] = useState('front');
  const [selectedZone, setSelectedZone] = useState('front');
  const [showPrintZone, setShowPrintZone] = useState(true);
  const [canvasLayers, setCanvasLayers] = useState([]);
  const [activeLayerId, setActiveLayerId] = useState(null);
  const [fabricDesignObject, setFabricDesignObject] = useState(null);
  const [designScale, setDesignScale] = useState(1);
  const [designOpacity, setDesignOpacity] = useState(1);

  // Section 5D — ULTRA Autonomous Design Generation state
  const [aiPrompt, setAiPrompt] = useState('');
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [aiQueueStatus, setAiQueueStatus] = useState(null);
  const [generatedImageUrl, setGeneratedImageUrl] = useState(null);
  const recentPromptLog = useRef([]);

  // Publishing state
  const [isPublishing, setIsPublishing] = useState(false);
  const [publishSuccessBanner, setPublishSuccessBanner] = useState(null);
  const [stripeBuyButtonId, setStripeBuyButtonId] = useState('');

  // Section 2C — Full separation: designLog vs transactionLedger
  const [designLog, setDesignLog] = useState([]); // AI generation history, canvas saves, approvals
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [transactionLedger, setTransactionLedger] = useState([]); // Real confirmed orders ONLY (Stripe + Printify IDs required)

  const canvasElRef = useRef(null);
  const fabricCanvasRef = useRef(null);

  const activePrintZone = showPrintZone
    ? PRINT_ZONE_BOUNDS[selectedZone] || PRINT_ZONE_BOUNDS.front
    : null;

  const computedSku = selectedBlueprint
    ? `POD-${selectedBlueprint.id}-${selectedVariant?.id || 'VAR'}-${selectedZone.toUpperCase()}`
    : `POD-STUDIO-${selectedZone.toUpperCase()}`;

  // On mount — load live Printify catalog
  useEffect(() => {
    fetch('/api/catalog')
      .then(r => r.json())
      .then(data => {
        if (Array.isArray(data)) {
          setCatalog(data);
          if (data.length > 0) setSelectedBlueprint(data[0]);
        }
      })
      .catch(err => console.error('[ULTRA] Catalog load error:', err.message));
  }, []);

  // When blueprint selected — load its variants
  useEffect(() => {
    if (!selectedBlueprint) return;
    fetch(`/api/catalog/${selectedBlueprint.id}/variants`)
      .then(r => r.json())
      .then(({ variants = [], providerId = null }) => {
        setBlueprintVariants(variants);
        setSelectedProviderId(providerId);
        setSelectedVariant(variants[0] || null); // Default to first variant
      })
      .catch(err => console.error('[ULTRA] Variants load error:', err.message));
  }, [selectedBlueprint]);

  // Initialize Fabric.js canvas (500x600)
  useEffect(() => {
    if (!canvasElRef.current) return;
    const canvas = new fabric.Canvas(canvasElRef.current, {
      width: 500,
      height: 600,
      preserveObjectStacking: true,
      selection: true
    });
    fabricCanvasRef.current = canvas;

    canvas.on('selection:created', e => {
      const active = e.selected?.[0];
      if (active) {
        setFabricDesignObject(active);
        setActiveLayerId(active.layerId || null);
        setDesignScale(active.scaleX || 1);
        setDesignOpacity(active.opacity ?? 1);
      }
    });

    canvas.on('selection:updated', e => {
      const active = e.selected?.[0];
      if (active) {
        setFabricDesignObject(active);
        setActiveLayerId(active.layerId || null);
        setDesignScale(active.scaleX || 1);
        setDesignOpacity(active.opacity ?? 1);
      }
    });

    canvas.on('selection:cleared', () => {
      setFabricDesignObject(null);
      setActiveLayerId(null);
    });

    return () => {
      canvas.dispose();
      fabricCanvasRef.current = null;
    };
  }, []);

  // Place transparent PNG onto Fabric.js canvas and register in canvasLayers
  const placeDesignOnCanvas = (imageUrl, name = null) => {
    return new Promise((resolve, reject) => {
      if (!fabricCanvasRef.current) return reject(new Error('Canvas not initialized'));
      fabric.Image.fromURL(
        imageUrl,
        img => {
          if (!img) return reject(new Error('Failed to load design image onto canvas'));
          const zone = PRINT_ZONE_BOUNDS[selectedZone] || PRINT_ZONE_BOUNDS.front;
          const layerId = `layer-${Date.now()}`;
          const targetWidth = Math.min(zone.width * 0.85, 220);
          const scaleFactor = img.width ? targetWidth / img.width : 0.5;

          img.set({
            left: zone.left + zone.width / 2,
            top: zone.top + zone.height / 2,
            originX: 'center',
            originY: 'center',
            scaleX: scaleFactor,
            scaleY: scaleFactor,
            opacity: 1,
            layerId
          });

          fabricCanvasRef.current.add(img);
          fabricCanvasRef.current.setActiveObject(img);
          fabricCanvasRef.current.renderAll();

          setFabricDesignObject(img);
          setActiveLayerId(layerId);
          setDesignScale(scaleFactor);
          setDesignOpacity(1);

          setCanvasLayers(prev => [
            ...prev,
            {
              id: layerId,
              name: name || `ULTRA Design ${prev.length + 1}`,
              thumbnail: imageUrl,
              fabricObj: img
            }
          ]);

          resolve(img);
        },
        { crossOrigin: 'anonymous' }
      );
    });
  };

  // Section 4E — Canvas Helper Functions
  const handleCenterDesign = () => {
    if (!fabricDesignObject || !activePrintZone) return;
    fabricDesignObject.set({
      left: activePrintZone.left + activePrintZone.width / 2,
      top: activePrintZone.top + activePrintZone.height / 2,
      originX: 'center',
      originY: 'center'
    });
    fabricCanvasRef.current.renderAll();
  };

  const handleFlipHorizontal = () => {
    if (!fabricDesignObject) return;
    fabricDesignObject.set('flipX', !fabricDesignObject.flipX);
    fabricCanvasRef.current.renderAll();
  };

  const handleFlipVertical = () => {
    if (!fabricDesignObject) return;
    fabricDesignObject.set('flipY', !fabricDesignObject.flipY);
    fabricCanvasRef.current.renderAll();
  };

  const handleRotate90 = () => {
    if (!fabricDesignObject) return;
    fabricDesignObject.set('angle', ((fabricDesignObject.angle || 0) + 90) % 360);
    fabricCanvasRef.current.renderAll();
  };

  const handleScaleChange = scale => {
    if (!fabricDesignObject) return;
    setDesignScale(scale);
    fabricDesignObject.set({ scaleX: scale, scaleY: scale });
    fabricCanvasRef.current.renderAll();
  };

  const handleOpacityChange = opacity => {
    if (!fabricDesignObject) return;
    setDesignOpacity(opacity);
    fabricDesignObject.set('opacity', opacity);
    fabricCanvasRef.current.renderAll();
  };

  const handleRemoveDesign = () => {
    if (!fabricDesignObject) return;
    const removedLayerId = fabricDesignObject.layerId;
    fabricCanvasRef.current.remove(fabricDesignObject);
    setFabricDesignObject(null);
    setGeneratedImageUrl(null);
    if (removedLayerId) {
      setCanvasLayers(prev => prev.filter(l => l.id !== removedLayerId));
    }
    fabricCanvasRef.current.renderAll();
  };

  const selectLayer = layer => {
    setActiveLayerId(layer.id);
    if (layer.fabricObj && fabricCanvasRef.current) {
      fabricCanvasRef.current.setActiveObject(layer.fabricObj);
      fabricCanvasRef.current.renderAll();
      setFabricDesignObject(layer.fabricObj);
      setDesignScale(layer.fabricObj.scaleX || 1);
      setDesignOpacity(layer.fabricObj.opacity ?? 1);
    }
  };

  const removeLayer = layerId => {
    const target = canvasLayers.find(l => l.id === layerId);
    if (target?.fabricObj && fabricCanvasRef.current) {
      fabricCanvasRef.current.remove(target.fabricObj);
      fabricCanvasRef.current.renderAll();
    }
    setCanvasLayers(prev => prev.filter(l => l.id !== layerId));
    if (activeLayerId === layerId) {
      setActiveLayerId(null);
      setFabricDesignObject(null);
    }
  };

  // Get garment mockup image URL from Printify blueprint
  const getGarmentMockupUrl = (blueprint, variant, view) => {
    const mockup = blueprint?.images?.find(
      img =>
        img?.src?.toLowerCase().includes(view) ||
        img?.variant_ids?.includes(variant?.id)
    );
    return mockup?.src || blueprint?.images?.[0]?.src || blueprint?.images?.[0] || null;
  };

  // Section 5D — Autonomous Prompt Generation & 3-Step Pipeline (Gemini -> HF FLUX -> RMBG-1.4)
  const handleQueueAiMockup = async () => {
    if (isGeneratingAi) return;
    setIsGeneratingAi(true);

    try {
      setAiQueueStatus('ULTRA architecting design concept...');

      // Step 1 — ULTRA writes its own prompt via Gemini
      const promptRes = await fetch('/api/generate-prompt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          selectedProduct: selectedBlueprint?.title || 'garment',
          selectedVariant: selectedVariant?.options,
          printZone: selectedZone,
          recentPrompts: recentPromptLog.current.slice(-5),
          userHint: aiPrompt || null
        })
      });
      if (!promptRes.ok) throw new Error(`Prompt gen failed: ${promptRes.status}`);
      const { generatedPrompt } = await promptRes.json();
      recentPromptLog.current.push(generatedPrompt);
      setAiQueueStatus(`Generating: "${generatedPrompt.slice(0, 55)}..."`);

      // Step 2 — Generate image via HF FLUX
      const imgRes = await fetch('/api/generate-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: generatedPrompt })
      });
      if (!imgRes.ok) throw new Error(`Image gen failed: ${imgRes.status}`);
      const { imageUrl } = await imgRes.json();

      // Step 3 — Strip background via RMBG-1.4
      setAiQueueStatus('Removing background...');
      const bgRes = await fetch('/api/remove-background', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageUrl })
      });
      const { transparentImageUrl } = await bgRes.json();

      // Step 4 — Place on canvas
      setGeneratedImageUrl(transparentImageUrl);
      await placeDesignOnCanvas(transparentImageUrl);

      const designEntry = {
        id: `design-${Date.now()}`,
        sku: computedSku,
        imageUrl: transparentImageUrl,
        prompt: generatedPrompt,
        blueprintId: selectedBlueprint?.id,
        variantId: selectedVariant?.id,
        zone: selectedZone,
        generatedAt: new Date().toISOString()
      };

      setPendingApprovals(prev => [...prev, designEntry]);
      setDesignLog(prev => [
        ...prev,
        {
          event: 'generated',
          designId: designEntry.id,
          sku: computedSku,
          prompt: generatedPrompt,
          timestamp: designEntry.generatedAt
        }
      ]);

      setAiQueueStatus('Design placed — approve to publish ✓');
    } catch (e) {
      setAiQueueStatus(`Error: ${e.message}`);
    } finally {
      setIsGeneratingAi(false);
    }
  };

  // Section 2C — Design approvals are creative events only; never touch transactionLedger
  const handleApproveDesign = async design => {
    const approvedAt = new Date().toISOString();
    await fetch('/api/log-design-approval', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        designId: design.id,
        sku: design.sku || computedSku,
        blueprintId: design.blueprintId,
        variantId: design.variantId,
        zone: design.zone,
        approvedAt
      })
    });
    setPendingApprovals(prev => prev.filter(item => item.id !== design.id));
    setDesignLog(prev => [
      ...prev,
      {
        event: 'approved',
        designId: design.id,
        sku: design.sku || computedSku,
        timestamp: approvedAt
      }
    ]);
  };

  // Publish live to Printify POD Pop-Up physical storefront via backend
  const handlePublishLive = async () => {
    if (isPublishing || !selectedBlueprint || !generatedImageUrl) return;
    setIsPublishing(true);
    try {
      const res = await fetch('/api/publish-product', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sku: computedSku,
          blueprintId: selectedBlueprint.id,
          productId: selectedBlueprint.id,
          providerId: selectedProviderId,
          variantId: selectedVariant?.id,
          zone: selectedZone,
          mockupImageUrl: generatedImageUrl
        })
      });
      if (!res.ok) throw new Error(`Publish failed: ${res.status}`);
      const data = await res.json();
      setPublishSuccessBanner({
        syncId: data.printifyProductId,
        storeUrl: data.storeUrl,
        sku: computedSku,
        timestamp: new Date().toISOString()
      });
      setDesignLog(prev => [
        ...prev,
        {
          event: 'published_to_printify',
          printifyProductId: data.printifyProductId,
          sku: computedSku,
          timestamp: new Date().toISOString()
        }
      ]);
      if (typeof onNavigateAway === 'function') {
        onNavigateAway('storefront');
      }
    } catch (e) {
      console.error('[ULTRA] Publish error:', e.message);
      setPublishSuccessBanner(null);
    } finally {
      setIsPublishing(false);
    }
  };

  // Section 2B — Transaction Guard on client ledger updates
  const writeClientTransaction = orderData => {
    const REQUIRED_FIELDS = ['printifyOrderId', 'stripePaymentId', 'amount', 'currency'];
    const missing = REQUIRED_FIELDS.filter(f => !orderData || !orderData[f]);
    if (missing.length > 0) {
      console.error(`[ULTRA] TRANSACTION BLOCKED — missing real fields: ${missing.join(', ')}`);
      throw new Error('Cannot write transaction without real Printify order ID and real Stripe payment ID');
    }
    setTransactionLedger(prev => [...prev, orderData]);
  };

  return (
    <div className="h-screen w-screen bg-gray-950 text-gray-100 flex flex-col overflow-hidden">
      {/* Top Studio Header */}
      <header className="h-12 bg-gray-900 border-b border-gray-800 px-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="px-2 py-0.5 text-xs font-bold rounded bg-purple-600 text-white">ULTRA</span>
          <h1 className="text-sm font-semibold tracking-wide text-white">
            Print Defy POD Pop-Up Studio · Printify Studio Clone
          </h1>
          <span className="text-xs text-gray-400 font-mono">{computedSku}</span>
        </div>

        <div className="flex items-center gap-3">
          {publishSuccessBanner && (
            <span className="text-xs text-emerald-400 font-mono">
              Published Printify ID: {publishSuccessBanner.syncId}
            </span>
          )}
          <button
            onClick={handlePublishLive}
            disabled={isPublishing || !selectedBlueprint || !generatedImageUrl}
            className="px-3 py-1.5 rounded bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-xs font-semibold text-white transition-all"
          >
            {isPublishing ? 'Publishing to Printify...' : 'Publish to Printify POD Pop-Up'}
          </button>
        </div>
      </header>

      {/* Main 3-Panel Printify Studio Clone Workspace */}
      <div className="flex-1 flex overflow-hidden">
        {/* 4B — LEFT PANEL: Full Printify Catalog Browser */}
        <div className="left-panel w-64 bg-gray-900 border-r border-gray-800 flex flex-col">
          {/* Category filter tabs */}
          <div className="category-tabs flex flex-wrap gap-1 p-2 border-b border-gray-700">
            {['All', 'T-Shirts', 'Hoodies', 'Hats', 'Bags', 'Accessories', 'Home'].map(cat => (
              <button
                key={cat}
                onClick={() => setCatalogCategory(cat.toLowerCase())}
                className={`px-2 py-1 text-xs rounded ${
                  catalogCategory === cat.toLowerCase()
                    ? 'bg-purple-600 text-white'
                    : 'bg-gray-800 text-gray-400 hover:bg-gray-700'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Product grid */}
          <div className="product-grid flex-1 overflow-y-auto p-2 grid grid-cols-2 gap-2">
            {catalog
              .filter(
                bp =>
                  catalogCategory === 'all' ||
                  bp.model?.toLowerCase().includes(catalogCategory) ||
                  bp.title?.toLowerCase().includes(catalogCategory)
              )
              .map(bp => (
                <button
                  key={bp.id}
                  onClick={() => setSelectedBlueprint(bp)}
                  className={`product-card rounded-lg overflow-hidden border-2 transition-all ${
                    selectedBlueprint?.id === bp.id ? 'border-purple-500' : 'border-transparent'
                  }`}
                >
                  <img
                    src={bp.images?.[0]?.src || bp.images?.[0]}
                    alt={bp.title}
                    className="w-full aspect-square object-cover"
                  />
                  <p className="text-xs text-gray-300 p-1 truncate">{bp.title}</p>
                </button>
              ))}
          </div>

          {/* Variant selector — shown when product selected */}
          {selectedBlueprint && (
            <div className="variant-panel border-t border-gray-700 p-3">
              <p className="text-xs text-gray-400 mb-2 font-semibold uppercase tracking-wider">
                Color
              </p>
              <div className="color-swatches flex flex-wrap gap-1 mb-3">
                {[...new Map(blueprintVariants.map(v => [v.options?.color, v])).values()].map(v => (
                  <button
                    key={v.options?.color || v.id}
                    title={v.options?.color}
                    onClick={() => setSelectedVariant(v)}
                    className={`w-6 h-6 rounded-full border-2 transition-all ${
                      selectedVariant?.options?.color === v.options?.color
                        ? 'border-purple-400 scale-110'
                        : 'border-gray-600'
                    }`}
                    style={{
                      backgroundColor: v.options?.color?.toLowerCase().replace(/\s/g, '') || '#111'
                    }}
                  />
                ))}
              </div>

              <p className="text-xs text-gray-400 mb-2 font-semibold uppercase tracking-wider">
                Size
              </p>
              <div className="size-pills flex flex-wrap gap-1">
                {[...new Set(blueprintVariants.map(v => v.options?.size))].filter(Boolean).map(size => (
                  <button
                    key={size}
                    onClick={() => {
                      const match = blueprintVariants.find(
                        v =>
                          v.options?.size === size &&
                          v.options?.color === selectedVariant?.options?.color
                      );
                      if (match) setSelectedVariant(match);
                    }}
                    className={`px-2 py-0.5 text-xs rounded border ${
                      selectedVariant?.options?.size === size
                        ? 'border-purple-400 text-purple-300 bg-purple-900/30'
                        : 'border-gray-600 text-gray-400'
                    }`}
                  >
                    {size}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* 4C — CENTER CANVAS: Printify Studio Equivalent */}
        <div className="center-canvas flex-1 flex flex-col items-center justify-center bg-gray-950 relative p-4 overflow-y-auto">
          {/* View toggle — front / back / left sleeve / right sleeve */}
          <div className="view-tabs flex gap-2 mb-3">
            {['front', 'back', 'left-sleeve', 'right-sleeve'].map(view => (
              <button
                key={view}
                onClick={() => setCanvasView(view)}
                className={`px-3 py-1 text-xs rounded capitalize ${
                  canvasView === view ? 'bg-purple-600 text-white' : 'bg-gray-800 text-gray-400'
                }`}
              >
                {view.replace('-', ' ')}
              </button>
            ))}
          </div>

          {/* Canvas container */}
          <div className="canvas-wrapper relative" style={{ width: 500, height: 600 }}>
            {/* Garment mockup background image */}
            {selectedBlueprint && (
              <img
                src={getGarmentMockupUrl(selectedBlueprint, selectedVariant, canvasView)}
                alt="Garment mockup"
                className="absolute inset-0 w-full h-full object-contain pointer-events-none"
              />
            )}

            {/* Print zone overlay — shows the printable area */}
            {activePrintZone && (
              <div
                className="print-zone-overlay absolute border-2 border-dashed border-purple-400/50 pointer-events-none"
                style={{
                  left: activePrintZone.left,
                  top: activePrintZone.top,
                  width: activePrintZone.width,
                  height: activePrintZone.height
                }}
              />
            )}

            {/* Fabric.js canvas — sits over the mockup */}
            <canvas
              ref={canvasElRef}
              className="absolute inset-0"
              style={{ width: 500, height: 600 }}
            />
          </div>

          {/* Canvas toolbar — Printify-style controls */}
          <div className="canvas-toolbar flex gap-2 mt-3 bg-gray-900 rounded-xl px-4 py-2">
            <button onClick={handleCenterDesign} title="Center" className="toolbar-btn">
              ⊞
            </button>
            <button onClick={handleFlipHorizontal} title="Flip H" className="toolbar-btn">
              ↔
            </button>
            <button onClick={handleFlipVertical} title="Flip V" className="toolbar-btn">
              ↕
            </button>
            <button onClick={handleRotate90} title="Rotate 90°" className="toolbar-btn">
              ↻
            </button>
            <div className="w-px bg-gray-700 mx-1" />
            <button
              onClick={handleRemoveDesign}
              title="Remove design"
              className="toolbar-btn text-red-400"
            >
              ✕
            </button>
          </div>

          {/* Scale/opacity sliders — Printify-style */}
          {fabricDesignObject && (
            <div className="design-adjustments flex gap-4 mt-2 text-xs text-gray-400">
              <label className="flex items-center gap-2">
                Scale
                <input
                  type="range"
                  min="5"
                  max="200"
                  value={Math.round(designScale * 100)}
                  onChange={e => handleScaleChange(e.target.value / 100)}
                  className="w-24 accent-purple-500"
                />
                <span>{Math.round(designScale * 100)}%</span>
              </label>
              <label className="flex items-center gap-2">
                Opacity
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={Math.round(designOpacity * 100)}
                  onChange={e => handleOpacityChange(e.target.value / 100)}
                  className="w-24 accent-purple-500"
                />
                <span>{Math.round(designOpacity * 100)}%</span>
              </label>
            </div>
          )}

          {/* Separated Design Log vs Verified Transaction Ledger Footer */}
          <div className="w-full max-w-2xl mt-4 grid grid-cols-2 gap-3 text-xs">
            <div className="bg-gray-900 border border-gray-800 rounded-lg p-3">
              <p className="font-semibold text-purple-300 mb-1">
                Creative Design Log ({designLog.length})
              </p>
              {pendingApprovals.map(item => (
                <div
                  key={item.id}
                  className="flex items-center justify-between py-1 border-b border-gray-800"
                >
                  <span className="truncate text-gray-300">{item.sku}</span>
                  <button
                    onClick={() => handleApproveDesign(item)}
                    className="px-2 py-0.5 rounded bg-purple-600 text-white text-[10px]"
                  >
                    Approve Design
                  </button>
                </div>
              ))}
              {designLog.length === 0 && pendingApprovals.length === 0 && (
                <p className="text-gray-500">No design events logged yet.</p>
              )}
            </div>

            <div className="bg-gray-900 border border-gray-800 rounded-lg p-3">
              <p className="font-semibold text-emerald-400 mb-1">
                Verified Transaction Ledger ({transactionLedger.length})
              </p>
              {transactionLedger.length === 0 ? (
                <p className="text-gray-500">
                  Awaiting live Stripe payment_intent.succeeded webhook + Printify order ID.
                </p>
              ) : (
                transactionLedger.map(tx => (
                  <div key={tx.stripePaymentId} className="py-1 border-b border-gray-800 font-mono">
                    {tx.printifyOrderId} · {tx.stripePaymentId} · {(tx.amount / 100).toFixed(2)}{' '}
                    {tx.currency?.toUpperCase()}
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* 4D — RIGHT PANEL: Layers + Controls */}
        <div className="right-panel w-56 bg-gray-900 border-l border-gray-800 flex flex-col">
          <div className="panel-header px-3 py-2 border-b border-gray-700">
            <h3 className="text-xs font-semibold text-gray-300 uppercase tracking-wider">Layers</h3>
          </div>

          {/* Layer list */}
          <div className="layers-list flex-1 overflow-y-auto p-2 flex flex-col gap-1">
            {/* Garment base layer — always locked at bottom */}
            <div className="layer-item flex items-center gap-2 px-2 py-1.5 rounded bg-gray-800">
              <span className="text-gray-500 text-xs">🔒</span>
              <span className="text-xs text-gray-400 flex-1 truncate">Garment Base</span>
            </div>

            {/* Print zone layer */}
            <div className="layer-item flex items-center gap-2 px-2 py-1.5 rounded bg-gray-800">
              <input
                type="checkbox"
                checked={showPrintZone}
                onChange={e => setShowPrintZone(e.target.checked)}
                className="accent-purple-500"
              />
              <span className="text-xs text-gray-400 flex-1 truncate">Print Zone Guide</span>
            </div>

            {/* Design layers — one per added design */}
            {canvasLayers.map((layer, i) => (
              <div
                key={layer.id}
                onClick={() => selectLayer(layer)}
                className={`layer-item flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer ${
                  activeLayerId === layer.id
                    ? 'bg-purple-900/40 border border-purple-500/30'
                    : 'bg-gray-800 hover:bg-gray-750'
                }`}
              >
                <img src={layer.thumbnail} alt="" className="w-6 h-6 object-cover rounded" />
                <span className="text-xs text-gray-300 flex-1 truncate">
                  {layer.name || `Design ${i + 1}`}
                </span>
                <button
                  onClick={e => {
                    e.stopPropagation();
                    removeLayer(layer.id);
                  }}
                  className="text-gray-600 hover:text-red-400 text-xs"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>

          {/* Print zone selector */}
          <div className="zone-selector border-t border-gray-700 p-3">
            <p className="text-xs text-gray-400 mb-2 font-semibold uppercase tracking-wider">
              Print Position
            </p>
            <div className="flex flex-col gap-1">
              {[
                'front',
                'back',
                'left-chest',
                'right-chest',
                'left-sleeve',
                'right-sleeve',
                'hood',
                'hem'
              ].map(zone => (
                <button
                  key={zone}
                  onClick={() => setSelectedZone(zone)}
                  className={`text-xs py-1 px-2 rounded text-left capitalize ${
                    selectedZone === zone
                      ? 'bg-purple-600/30 text-purple-300 border border-purple-500/30'
                      : 'text-gray-500 hover:text-gray-300 hover:bg-gray-800'
                  }`}
                >
                  {zone.replace(/-/g, ' ')}
                </button>
              ))}
            </div>
          </div>

          {/* ULTRA AI generate button */}
          <div className="ai-panel border-t border-gray-700 p-3 flex flex-col gap-2">
            <input
              type="text"
              value={aiPrompt}
              onChange={e => setAiPrompt(e.target.value)}
              placeholder="Optional ULTRA style hint..."
              className="w-full px-2 py-1 text-xs rounded bg-gray-800 border border-gray-700 text-gray-200 focus:outline-none focus:border-purple-500"
            />
            <button
              onClick={handleQueueAiMockup}
              disabled={isGeneratingAi}
              className="w-full py-2 px-3 rounded-lg bg-gradient-to-r from-purple-700 to-indigo-700 hover:from-purple-600 hover:to-indigo-600 text-white text-xs font-semibold disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {isGeneratingAi ? '⚡ ULTRA Generating...' : '⚡ ULTRA: Generate Design'}
            </button>
            {aiQueueStatus && (
              <p className="text-xs text-purple-300 mt-1 text-center leading-tight">
                {aiQueueStatus}
              </p>
            )}
          </div>

          {/* Optional Stripe Buy Button Embed Section */}
          <div className="border-t border-gray-700 p-3">
            <input
              type="text"
              value={stripeBuyButtonId}
              onChange={e => setStripeBuyButtonId(e.target.value)}
              placeholder="Stripe buy-button-id..."
              className="w-full px-2 py-1 text-xs rounded bg-gray-800 border border-gray-700 text-gray-200 mb-2"
            />
            {stripeBuyButtonId && <StripeBuyButton buyButtonId={stripeBuyButtonId} />}
          </div>
        </div>
      </div>
    </div>
  );
}

export default PodStudioDesktopWorkspace;
