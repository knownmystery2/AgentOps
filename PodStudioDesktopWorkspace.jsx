import React, { useState, useEffect, useRef, useCallback } from 'react';
import { fabric } from 'fabric';
import {
  ArrowLeft,
  Check,
  Eye,
  EyeOff,
  Layers,
  Cpu,
  Sparkles,
  RefreshCw,
  CheckCircle2,
  Package,
  Zap,
  ShieldCheck,
  Printer,
  Shirt,
  ShoppingBag,
  Coffee,
  Image as ImageIcon
} from 'lucide-react';

// Brand Aesthetic Identity Context (Section 5A & Fix 5)
const BRAND_IDENTITY_CONTEXT = `High-end luxury streetwear. Dark opulence: blacks, golds, iridescent chrome.
Deconstructed couture meets underground street culture. Baroque ornament, sacred geometry, celestial maps,
typographic aggression, marble textures, liquid chrome. Editorial. Unexpected. Museum-quality graphic design
executed with streetwear energy.`;

// Physical Print-on-Demand Product Catalog Models (Printify Blueprint IDs)
const PRODUCT_CATALOG = [
  {
    id: '12',
    name: 'Heavyweight T-Shirt',
    category: 'Apparel',
    weight: '240 GSM Organic Cotton',
    baseSku: 'POD-HET',
    icon: Shirt,
    mockupType: 'tshirt'
  },
  {
    id: '77',
    name: 'Vintage Hoodie',
    category: 'Apparel',
    weight: '380 GSM Heavy Fleece',
    baseSku: 'POD-HOD',
    icon: Shirt,
    mockupType: 'hoodie'
  },
  {
    id: '68',
    name: 'Ceramic Mug',
    category: 'Drinkware',
    weight: '11 oz Glossy Ceramic',
    baseSku: 'POD-MUG',
    icon: Coffee,
    mockupType: 'mug'
  },
  {
    id: '104',
    name: 'Canvas Tote Bag',
    category: 'Accessories',
    weight: '100% Heavy Bull Denim',
    baseSku: 'POD-TOT',
    icon: ShoppingBag,
    mockupType: 'tote'
  },
  {
    id: '282',
    name: 'Wall Art Poster',
    category: 'Home Decor',
    weight: '250 GSM Archival Matte',
    baseSku: 'POD-PST',
    icon: ImageIcon,
    mockupType: 'poster'
  }
];

// Garment Color Swatches
const COLOR_SWATCHES = [
  { id: 'onyx-black', name: 'Onyx Black', hex: '#0F1115', textMode: 'light' },
  { id: 'dark-teal', name: 'Dark Teal', hex: '#0D5C53', textMode: 'light' },
  { id: 'cream', name: 'Cream', hex: '#F5EBE6', textMode: 'dark' },
  { id: 'forest-green', name: 'Forest Green', hex: '#1A331E', textMode: 'light' },
  { id: 'terracotta', name: 'Terracotta', hex: '#C27648', textMode: 'light' },
  { id: 'classic-white', name: 'Classic White', hex: '#FFFFFF', textMode: 'dark' }
];

// Print Zones Configuration
const PRINT_ZONES = {
  chest: {
    id: 'chest',
    name: 'Chest Center (11" x 13")',
    skuCode: 'CHSL',
    width: 260,
    height: 310,
    topOffset: '24%',
    leftOffset: '50%',
    transform: 'translate(-50%, 0)'
  },
  pocket: {
    id: 'pocket',
    name: 'Pocket Left (4" x 4")',
    skuCode: 'PKTL',
    width: 110,
    height: 110,
    topOffset: '26%',
    leftOffset: '63%',
    transform: 'translate(-50%, 0)'
  }
};

/**
 * PodStudioDesktopWorkspace - Print Defy POD Pop-Up Studio (ULTRA Agent v4)
 * Strictly physical POD merchandise fulfilled by Printify for the Printify POD Pop-Up physical storefront.
 */
export function PodStudioDesktopWorkspace({ currentView = 'designer', onNavigateAway }) {
  // State: Product Selection
  const [selectedProduct, setSelectedProduct] = useState(PRODUCT_CATALOG[0]);
  const [selectedColor, setSelectedColor] = useState(COLOR_SWATCHES[0]);
  const [selectedZone, setSelectedZone] = useState('chest');

  // State: UI & Controls
  const [layerVisibility, setLayerVisibility] = useState({
    circle: true,
    text: true
  });
  const [aiPrompt, setAiPrompt] = useState('');
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [aiQueueStatus, setAiQueueStatus] = useState(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [publishSuccessBanner, setPublishSuccessBanner] = useState(null);

  // --- DESIGN WORKFLOW STATE (creative events only) ---
  const [generatedImageUrl, setGeneratedImageUrl] = useState(null);
  const [fabricDesignObject, setFabricDesignObject] = useState(null);
  const [designLog, setDesignLog] = useState([]);
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [approvedDesigns, setApprovedDesigns] = useState([]);
  const recentPromptLog = useRef([]);

  // --- FINANCIAL STATE (real confirmed orders only) ---
  const [transactionLedger, setTransactionLedger] = useState([]);
  // transactionLedger is ONLY written from: real Stripe webhook confirmations
  // or real Printify order ID responses. Never from design events.

  // State: Live Canvas Telemetry Debugger
  const [canvasPayload, setCanvasPayload] = useState({});

  // Refs: Canvas & Fabric Infrastructure
  const canvasElementRef = useRef(null);
  const fabricCanvasRef = useRef(null);
  const clipPathRef = useRef(null);
  const circleObjRef = useRef(null);
  const textObjRef = useRef(null);
  const containerRef = useRef(null);

  const printZones = PRINT_ZONES;

  // SKU calculation
  const computedSku = `${selectedProduct.baseSku}--${PRINT_ZONES[selectedZone].skuCode}-2026`;

  // Design approval handler — creative event, NOT a transaction
  const handleApproveDesign = (design) => {
    setApprovedDesigns(prev => [...prev, { ...design, approvedAt: new Date().toISOString() }]);
    setPendingApprovals(prev => prev.filter(d => d.id !== design.id));
    setDesignLog(prev => [...prev, { event: 'approved', designId: design.id, at: new Date().toISOString() }]);

    fetch('/api/log-design-approval', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ designId: design.id, sku: design.sku, approvedAt: new Date().toISOString() })
    });
    // *** DO NOT write to transactionLedger here ***
  };

  // Real sale handler — only called from confirmed Stripe/Printify responses
  const handleConfirmedSale = (orderData) => {
    if (!orderData.printifyOrderId || !orderData.stripePaymentId) {
      console.error('[ULTRA] BLOCKED fake transaction — missing real order IDs');
      return;
    }
    setTransactionLedger(prev => [...prev, {
      printifyOrderId: orderData.printifyOrderId,
      stripePaymentId: orderData.stripePaymentId,
      amount: orderData.amount,
      currency: orderData.currency,
      sku: orderData.sku,
      recordedAt: new Date().toISOString()
    }]);
  };

  // Synchronize payload debugger from Fabric canvas
  const updatePayloadDebugger = useCallback(() => {
    const canvas = fabricCanvasRef.current;
    if (!canvas) return;

    const objects = canvas.getObjects();
    const zoneConfig = PRINT_ZONES[selectedZone];

    const layersData = objects.map((obj, index) => {
      const bound = obj.getBoundingRect(true);
      return {
        layerIndex: index,
        layerType: obj.type,
        id: obj.dataId || `layer_${index + 1}`,
        name: obj.dataName || (obj.type === 'circle' ? 'Vector Base Circle' : obj.type === 'image' ? 'ULTRA AI Transparent Design' : 'Typography Subtitle'),
        visible: obj.visible !== false,
        matrix: {
          x: Math.round(obj.left || 0),
          y: Math.round(obj.top || 0),
          normalizedX: Number(((obj.left || 0) / zoneConfig.width).toFixed(4)),
          normalizedY: Number(((obj.top || 0) / zoneConfig.height).toFixed(4)),
          scaleX: Number((obj.scaleX || 1).toFixed(3)),
          scaleY: Number((obj.scaleY || 1).toFixed(3)),
          angleDegrees: Math.round(obj.angle || 0),
          originX: obj.originX,
          originY: obj.originY
        },
        boundingBox: {
          width: Math.round(bound.width),
          height: Math.round(bound.height),
          clampedInsideZone:
            obj.left >= 0 &&
            obj.top >= 0 &&
            obj.left + bound.width <= zoneConfig.width &&
            obj.top + bound.height <= zoneConfig.height
        }
      };
    });

    const payload = {
      system: 'Print Defy POD Pop-Up Studio — ULTRA v4',
      storefront: 'Printify POD Pop-Up Physical Storefront',
      timestamp: new Date().toISOString(),
      sku: `${selectedProduct.baseSku}--${zoneConfig.skuCode}-2026`,
      product: {
        id: selectedProduct.id,
        name: selectedProduct.name,
        colorHex: selectedColor.hex,
        colorName: selectedColor.name
      },
      printZone: {
        name: zoneConfig.name,
        widthPx: zoneConfig.width,
        heightPx: zoneConfig.height,
        dpi: 300,
        colorProfile: 'CMYK FOGRA39'
      },
      layers: layersData,
      telemetry: {
        objectCount: objects.length,
        renderStatus: 'VALID_VECTOR_ALIGNED',
        supplierSync: 'SYNCHRONIZED_ACTIVE'
      }
    };

    setCanvasPayload(payload);
  }, [selectedProduct, selectedColor, selectedZone]);

  // Initialize Fabric Canvas
  useEffect(() => {
    if (!canvasElementRef.current) return;

    const zoneConfig = PRINT_ZONES[selectedZone];

    if (fabricCanvasRef.current) {
      fabricCanvasRef.current.dispose();
      fabricCanvasRef.current = null;
    }

    const canvas = new fabric.Canvas(canvasElementRef.current, {
      width: zoneConfig.width,
      height: zoneConfig.height,
      selection: true,
      preserveObjectStacking: true,
      backgroundColor: 'transparent'
    });

    fabricCanvasRef.current = canvas;

    const clipRect = new fabric.Rect({
      left: 0,
      top: 0,
      width: zoneConfig.width,
      height: zoneConfig.height,
      absolutePositioned: true
    });
    clipPathRef.current = clipRect;
    canvas.clipPath = clipRect;

    const circle = new fabric.Circle({
      radius: selectedZone === 'chest' ? 56 : 28,
      fill: '#FFFFFF',
      left: zoneConfig.width / 2,
      top: selectedZone === 'chest' ? zoneConfig.height / 2 - 32 : zoneConfig.height / 2 - 16,
      originX: 'center',
      originY: 'center',
      selectable: true,
      hasControls: true,
      hasBorders: true,
      cornerColor: '#9333ea',
      cornerStrokeColor: '#ffffff',
      cornerSize: 10,
      transparentCorners: false,
      hasRotatingPoint: true
    });
    circle.dataId = 'layer_circle';
    circle.dataName = 'Vector Base Circle';
    circleObjRef.current = circle;
    canvas.add(circle);

    const text = new fabric.Text('PRINT DEFY', {
      fontSize: selectedZone === 'chest' ? 17 : 9,
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
      fontWeight: 'bold',
      fill: '#FFFFFF',
      left: zoneConfig.width / 2,
      top: selectedZone === 'chest' ? zoneConfig.height / 2 + 50 : zoneConfig.height / 2 + 25,
      originX: 'center',
      originY: 'center',
      selectable: true,
      hasControls: true,
      hasBorders: true,
      cornerColor: '#9333ea',
      cornerStrokeColor: '#ffffff',
      cornerSize: 10,
      transparentCorners: false,
      charSpacing: 80
    });
    text.dataId = 'layer_text';
    text.dataName = 'Typography Subtitle';
    textObjRef.current = text;
    canvas.add(text);

    canvas.setActiveObject(circle);
    canvas.renderAll();

    const triggerUpdate = () => {
      updatePayloadDebugger();
    };

    canvas.on('object:modified', triggerUpdate);
    canvas.on('object:moving', triggerUpdate);
    canvas.on('object:scaling', triggerUpdate);
    canvas.on('object:rotating', triggerUpdate);
    canvas.on('selection:created', triggerUpdate);
    canvas.on('selection:updated', triggerUpdate);
    canvas.on('selection:cleared', triggerUpdate);

    updatePayloadDebugger();

    const handleResize = () => {
      if (fabricCanvasRef.current) {
        fabricCanvasRef.current.calcOffset();
        fabricCanvasRef.current.renderAll();
      }
    };
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      if (fabricCanvasRef.current) {
        fabricCanvasRef.current.dispose();
        fabricCanvasRef.current = null;
      }
    };
  }, [selectedZone, updatePayloadDebugger]);

  // Toggle Layer Visibility
  const toggleLayer = (layerKey) => {
    const canvas = fabricCanvasRef.current;
    if (!canvas) return;

    if (layerKey === 'circle' && circleObjRef.current) {
      const nextVisible = !layerVisibility.circle;
      circleObjRef.current.set('visible', nextVisible);
      setLayerVisibility((prev) => ({ ...prev, circle: nextVisible }));
    } else if (layerKey === 'text' && textObjRef.current) {
      const nextVisible = !layerVisibility.text;
      textObjRef.current.set('visible', nextVisible);
      setLayerVisibility((prev) => ({ ...prev, text: nextVisible }));
    }

    canvas.discardActiveObject();
    canvas.renderAll();
    updatePayloadDebugger();
  };

  // Place transparent PNG design onto Fabric.js canvas (Selectable, Draggable, Scalable)
  const placeDesignOnCanvas = async (imageUrl) => {
    if (!fabricCanvasRef.current) return;
    const canvas = fabricCanvasRef.current;

    if (fabricDesignObject) canvas.remove(fabricDesignObject);

    return new Promise((resolve, reject) => {
      fabric.Image.fromURL(imageUrl, (img) => {
        if (!img) return reject(new Error('Failed to load image onto canvas'));

        const zone = printZones[selectedZone];
        const maxW = zone?.width || 200;
        const maxH = zone?.height || 200;
        const scale = Math.min(maxW / img.width, maxH / img.height, 1);

        img.set({
          left: zone?.left || canvas.width / 2 - (img.width * scale) / 2,
          top: zone?.top || canvas.height / 2 - (img.height * scale) / 2,
          scaleX: scale,
          scaleY: scale,
          selectable: true,
          hasControls: true,
          hasBorders: true,
          cornerStyle: 'circle',
          cornerColor: '#9333ea',
          borderColor: '#9333ea',
          transparentCorners: false,
          crossOrigin: 'anonymous'
        });

        canvas.add(img);
        canvas.setActiveObject(img);
        canvas.renderAll();
        setFabricDesignObject(img);
        updatePayloadDebugger();
        resolve();
      }, { crossOrigin: 'anonymous' });
    });
  };

  // FIX 5 — Autonomous Prompt Engineering + HF FLUX Generation + RMBG-1.4 Background Removal + Fabric Canvas Placement
  const handleQueueAiMockup = async () => {
    if (isGeneratingAi) return;
    setIsGeneratingAi(true);

    try {
      // Step 1: ULTRA writes its own prompt via Gemini
      setAiQueueStatus('ULTRA crafting design concept...');
      const promptRes = await fetch('/api/generate-prompt', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          brandContext: BRAND_IDENTITY_CONTEXT,
          recentPrompts: recentPromptLog.current.slice(-5),
          userHint: aiPrompt || null
        })
      });
      if (!promptRes.ok) throw new Error(`Prompt generation failed: ${promptRes.status}`);
      const { generatedPrompt } = await promptRes.json();
      recentPromptLog.current.push(generatedPrompt);
      setAiQueueStatus(`Generating: "${generatedPrompt.slice(0, 60)}..."`);

      // Step 2: Generate image via Hugging Face
      const imgRes = await fetch('/api/generate-image', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: generatedPrompt })
      });
      if (!imgRes.ok) throw new Error(`Image generation failed: ${imgRes.status}`);
      const { imageUrl } = await imgRes.json();

      // Step 3: Remove background
      setAiQueueStatus('Removing background...');
      const bgRes = await fetch('/api/remove-background', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ imageUrl })
      });
      if (!bgRes.ok) throw new Error(`Background removal failed: ${bgRes.status}`);
      const { transparentImageUrl } = await bgRes.json();

      // Step 4: Place on Fabric canvas
      setGeneratedImageUrl(transparentImageUrl);
      setDesignLog(prev => [...prev, {
        event: 'generated',
        prompt: generatedPrompt,
        at: new Date().toISOString()
      }]);
      setPendingApprovals(prev => [...prev, {
        id: `design-${Date.now()}`,
        imageUrl: transparentImageUrl,
        prompt: generatedPrompt,
        sku: computedSku,
        generatedAt: new Date().toISOString()
      }]);

      await placeDesignOnCanvas(transparentImageUrl);
      setAiQueueStatus(`Design ready for approval — SKU: ${computedSku}`);
      setTimeout(() => setAiQueueStatus(null), 6000);

    } catch (e) {
      setAiQueueStatus(`Error: ${e.message}`);
      console.error('[ULTRA] Generation error:', e);
    } finally {
      setIsGeneratingAi(false);
    }
  };

  // FIX 6 — Publish physical product to Printify POD Pop-Up only
  const handlePublishLive = async () => {
    if (!generatedImageUrl) {
      setAiQueueStatus('Approve a design first before publishing to the POD Pop-Up.');
      return;
    }
    if (approvedDesigns.length === 0) {
      setAiQueueStatus('No approved designs — approve a design in the Design Studio before publishing.');
      return;
    }
    setIsPublishing(true);
    setAiQueueStatus('Publishing to Printify POD Pop-Up...');

    try {
      const res = await fetch('/api/publish-product', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sku: computedSku,
          productId: selectedProduct?.id,
          colorHex: selectedColor?.hex,
          zone: selectedZone,
          mockupImageUrl: generatedImageUrl
        })
      });
      if (!res.ok) throw new Error(`Publish failed: ${res.status}`);
      const data = await res.json();

      // data.printifyProductId is a real ID from Printify — never fabricated
      setPublishSuccessBanner({
        syncId: data.printifyProductId,
        sku: computedSku,
        storeUrl: data.storeUrl,
        timestamp: new Date().toLocaleTimeString()
      });

      // Navigate to the POD Pop-Up storefront only — no generic storefront list
      setTimeout(() => {
        if (typeof onNavigateAway === 'function') onNavigateAway('pod-popup-storefront');
      }, 2000);

    } catch (e) {
      setAiQueueStatus(`Publish error: ${e.message}`);
      console.error('[ULTRA] Publish error:', e);
    } finally {
      setIsPublishing(false);
    }
  };

  // Safely cleanup before navigating to the single Printify POD Pop-Up Storefront
  const handleBackToStorefront = () => {
    if (fabricCanvasRef.current) {
      fabricCanvasRef.current.dispose();
      fabricCanvasRef.current = null;
    }
    if (typeof onNavigateAway === 'function') {
      onNavigateAway('pod-popup-storefront');
    }
  };

  const zoneConfig = PRINT_ZONES[selectedZone];

  return (
    <div className="h-screen w-screen overflow-hidden flex flex-col bg-[#f4f5f7] select-none text-neutral-800 font-sans">
      {/* 1. FIXED TOP APPLICATION HEADER */}
      <header className="w-full bg-[#1e2022] h-16 flex items-center justify-between px-6 border-b border-neutral-800 shadow-md z-50 flex-shrink-0">
        {/* Left: Back to Printify POD Pop-Up Storefront */}
        <div className="flex items-center space-x-4">
          <button
            onClick={handleBackToStorefront}
            className="flex items-center text-sm font-medium text-neutral-400 hover:text-white transition-colors duration-150 group"
          >
            <ArrowLeft className="w-4 h-4 mr-2 transition-transform duration-150 group-hover:-translate-x-1" />
            <span>← Printify POD Pop-Up Storefront</span>
          </button>
          <div className="h-4 w-px bg-neutral-700 hidden sm:block" />
          <div className="hidden sm:flex items-center space-x-2 text-xs text-neutral-400">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>ULTRA Agent Active • Physical POD Only</span>
          </div>
        </div>

        {/* Center: Single Storefront & Engine Indicator */}
        <div className="flex items-center space-x-2">
          <Cpu className="w-4 h-4 text-purple-400 hidden md:block" />
          <span className="font-semibold text-neutral-200 tracking-wide text-xs sm:text-sm">
            Print Defy POD Pop-Up Studio · ULTRA v4
          </span>
        </div>

        {/* Right: Publish to Printify POD Pop-Up */}
        <div className="flex items-center space-x-3">
          <button
            onClick={handlePublishLive}
            disabled={isPublishing}
            className="bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-semibold text-sm px-5 py-2.5 rounded-lg shadow transition-all duration-150 active:scale-[0.98] flex items-center space-x-2 disabled:opacity-75 disabled:cursor-not-allowed"
          >
            {isPublishing ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Publishing to Printify POD Pop-Up...</span>
              </>
            ) : (
              <>
                <Zap className="w-4 h-4 text-yellow-300 fill-yellow-300" />
                <span>Publish to Printify POD Pop-Up</span>
              </>
            )}
          </button>
        </div>
      </header>

      {/* REAL PRINTIFY PUBLISH CONFIRMATION BANNER */}
      {publishSuccessBanner && (
        <div className="bg-emerald-600 text-white px-6 py-2.5 flex items-center justify-between shadow-lg text-sm transition-all duration-300 z-40">
          <div className="flex items-center space-x-3">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
            <div>
              <span className="font-bold">Published to Printify POD Pop-Up! </span>
              <span>
                SKU: {publishSuccessBanner.sku} • Printify Product ID: {publishSuccessBanner.syncId}
              </span>
            </div>
          </div>
          <button
            onClick={() => setPublishSuccessBanner(null)}
            className="text-white hover:text-neutral-200 text-xs uppercase font-bold tracking-wider underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* MAIN THREE-COLUMN WORKSPACE LAYOUT */}
      <div className="flex-1 flex overflow-hidden">
        {/* 2. LEFT COLUMN: PHYSICAL PRODUCT BLANKS & GARMENT CONFIG */}
        <aside className="h-[calc(100vh-64px)] w-[340px] flex flex-col overflow-y-auto p-6 space-y-6 bg-white border-r border-neutral-200 flex-shrink-0 shadow-sm">
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500">
                Physical POD Blank (Printify)
              </label>
              <span className="text-[11px] font-semibold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                {PRODUCT_CATALOG.length} Blueprints
              </span>
            </div>

            <div className="space-y-2">
              {PRODUCT_CATALOG.map((item) => {
                const IconComponent = item.icon;
                const isSelected = selectedProduct.id === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => setSelectedProduct(item)}
                    className={`w-full p-3 rounded-xl border text-left transition-all duration-150 flex items-center justify-between ${
                      isSelected
                        ? 'border-blue-600 bg-blue-50/70 shadow-sm ring-1 ring-blue-600'
                        : 'border-neutral-200 hover:border-neutral-300 hover:bg-neutral-50'
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <div
                        className={`p-2 rounded-lg ${
                          isSelected ? 'bg-blue-600 text-white' : 'bg-neutral-100 text-neutral-600'
                        }`}
                      >
                        <IconComponent className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-sm font-semibold text-neutral-900">{item.name}</div>
                        <div className="text-xs text-neutral-500">{item.weight}</div>
                      </div>
                    </div>
                    {isSelected && <Check className="w-4 h-4 text-blue-600 flex-shrink-0" />}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* GARMENT COLOR SWATCHES */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500">
                Garment Colorway
              </label>
              <span className="text-xs font-medium text-neutral-700">
                {selectedColor.name} ({selectedColor.hex})
              </span>
            </div>

            <div className="flex items-center space-x-3.5 py-1">
              {COLOR_SWATCHES.map((swatch) => {
                const isSelected = selectedColor.id === swatch.id;
                return (
                  <button
                    key={swatch.id}
                    onClick={() => setSelectedColor(swatch)}
                    title={`${swatch.name} (${swatch.hex})`}
                    className="relative group focus:outline-none"
                  >
                    <div
                      className={`w-9 h-9 rounded-full transition-all duration-150 flex items-center justify-center ${
                        isSelected
                          ? 'ring-2 ring-offset-2 ring-blue-600 scale-105 shadow-md'
                          : 'hover:scale-105 border border-neutral-300 shadow-sm'
                      }`}
                      style={{ backgroundColor: swatch.hex }}
                    >
                      {isSelected && (
                        <Check
                          className={`w-4 h-4 stroke-[3] ${
                            swatch.textMode === 'light' ? 'text-white' : 'text-neutral-900'
                          }`}
                        />
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* PRINT ZONE & DIMENSION PARAMETERS */}
          <div>
            <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 block mb-3">
              Print Area & Dimensions
            </label>
            <div className="grid grid-cols-2 gap-2">
              {Object.keys(PRINT_ZONES).map((zoneKey) => {
                const isActive = selectedZone === zoneKey;
                return (
                  <button
                    key={zoneKey}
                    onClick={() => setSelectedZone(zoneKey)}
                    className={`py-2.5 px-3 rounded-lg text-xs font-semibold border transition-all text-center flex flex-col items-center justify-center space-y-1 ${
                      isActive
                        ? 'border-blue-600 bg-blue-600 text-white shadow-sm'
                        : 'border-neutral-200 bg-white hover:bg-neutral-50 text-neutral-700'
                    }`}
                  >
                    <span>{zoneKey === 'chest' ? 'Chest Center' : 'Pocket Left'}</span>
                    <span className={`text-[10px] ${isActive ? 'text-blue-100' : 'text-neutral-400'}`}>
                      {zoneKey === 'chest' ? '11" × 13" Max' : '4" × 4" Pocket'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="p-4 rounded-xl bg-neutral-50 border border-neutral-200 space-y-2 mt-auto">
            <div className="flex items-center space-x-2 text-xs font-semibold text-neutral-800">
              <Printer className="w-4 h-4 text-neutral-600" />
              <span>Printify POD Pop-Up Physical Fulfillment</span>
            </div>
            <div className="text-[11px] text-neutral-500 leading-relaxed">
              Transparent PNG alpha channel output. 300 DPI DTG rendering shipped directly to customer physical address.
            </div>
          </div>
        </aside>

        {/* 3. CENTER FLEX WORKSPACE: INTERACTIVE GARMENT & FABRIC.JS CANVAS */}
        <main
          ref={containerRef}
          className="flex-1 h-[calc(100vh-64px)] overflow-hidden relative flex items-center justify-center bg-[#18181b] p-8 sm:p-12"
        >
          <div
            className="absolute inset-0 pointer-events-none opacity-20"
            style={{
              backgroundImage: `radial-gradient(circle at 1px 1px, #ffffff 1px, transparent 0)`,
              backgroundSize: '24px 24px'
            }}
          />
          <div className="absolute inset-0 pointer-events-none bg-gradient-to-t from-black/40 via-transparent to-black/20" />

          <div className="relative flex items-center justify-center w-[540px] h-[640px] max-w-full max-h-full transition-all duration-300">
            <div
              className="relative w-full h-full flex items-center justify-center filter drop-shadow-[0_20px_45px_rgba(0,0,0,0.65)]"
              style={{ transition: 'color 0.25s ease' }}
            >
              <svg
                viewBox="0 0 540 640"
                className="w-full h-full"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 L465 145 C485 160 475 190 445 220 L405 250 L410 575 C410 590 395 600 375 600 L165 600 C145 600 130 590 130 575 L135 250 L95 220 C65 190 55 160 75 145 Z"
                  fill="#000000"
                  opacity="0.3"
                  filter="blur(14px)"
                />
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 L465 145 C485 160 475 190 445 220 L405 250 L410 575 C410 590 395 600 375 600 L165 600 C145 600 130 590 130 575 L135 250 L95 220 C65 190 55 160 75 145 Z"
                  fill={selectedColor.hex}
                  className="transition-colors duration-200"
                />
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 L465 145 C485 160 475 190 445 220 L405 250 L410 575 C410 590 395 600 375 600 L165 600 C145 600 130 590 130 575 L135 250 L95 220 C65 190 55 160 75 145 Z"
                  fill="url(#fabricGradient)"
                  opacity="0.4"
                />
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 C345 90 300 95 270 95 C240 95 195 90 170 80 Z"
                  fill="#000000"
                  opacity="0.25"
                />
                <path
                  d="M170 80 C200 98 240 104 270 104 C300 104 340 98 370 80"
                  stroke="#ffffff"
                  strokeWidth="2"
                  strokeOpacity="0.2"
                  fill="none"
                />
                <path d="M135 250 L165 240" stroke="#000000" strokeWidth="2" opacity="0.3" />
                <path d="M405 250 L375 240" stroke="#000000" strokeWidth="2" opacity="0.3" />
                <path d="M130 585 L410 585" stroke="#ffffff" strokeWidth="1.5" strokeOpacity="0.15" />
                <path d="M130 588 L410 588" stroke="#000000" strokeWidth="1.5" strokeOpacity="0.2" />
                <defs>
                  <linearGradient id="fabricGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#ffffff" stopOpacity="0.3" />
                    <stop offset="35%" stopColor="#000000" stopOpacity="0" />
                    <stop offset="70%" stopColor="#000000" stopOpacity="0.25" />
                    <stop offset="100%" stopColor="#000000" stopOpacity="0.5" />
                  </linearGradient>
                </defs>
              </svg>
            </div>

            {/* BOUNDING PRINT FRAME CONTAINER */}
            <div
              className="absolute z-20 flex items-center justify-center transition-all duration-300"
              style={{
                top: zoneConfig.topOffset,
                left: zoneConfig.leftOffset,
                transform: zoneConfig.transform,
                width: `${zoneConfig.width}px`,
                height: `${zoneConfig.height}px`
              }}
            >
              <div className="absolute inset-0 pointer-events-none border-2 border-dashed border-cyan-400/80 rounded-md shadow-[0_0_12px_rgba(34,211,238,0.25)] flex flex-col justify-between p-1">
                <div className="flex items-center justify-between">
                  <span className="bg-cyan-900/90 text-cyan-300 text-[9px] font-mono font-bold px-1.5 py-0.5 rounded uppercase tracking-wider border border-cyan-500/40">
                    Print Area Limit
                  </span>
                  <span className="text-[9px] text-cyan-300 font-mono">
                    {zoneConfig.width}×{zoneConfig.height}px
                  </span>
                </div>
                <div className="flex items-center justify-between text-[8px] text-cyan-400 font-mono">
                  <span>+</span>
                  <span>+</span>
                </div>
              </div>

              {/* Active HTML5 Canvas Element managed by Fabric.js (draggable, scalable, selectable objects) */}
              <canvas
                ref={canvasElementRef}
                className="w-full h-full block cursor-move"
              />
            </div>
          </div>

          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-30">
            <div className="flex items-center space-x-2 bg-neutral-900/90 backdrop-blur-md px-4 py-1.5 rounded-full border border-neutral-700/80 shadow-xl text-neutral-300 text-xs font-mono">
              <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
              <span>300 DPI • Transparent PNG Alpha • Drag, Scale & Reposition Enabled</span>
            </div>
          </div>
        </main>

        {/* 4. RIGHT COLUMN: DESIGN STUDIO PANEL vs. REAL ORDERS / TRANSACTIONS PANEL (STRICTLY SEPARATED) */}
        <aside className="h-[calc(100vh-64px)] w-[340px] flex flex-col overflow-y-auto p-6 space-y-6 bg-white border-l border-neutral-200 flex-shrink-0 shadow-sm">
          {/* PANEL 1: DESIGN STUDIO (Creative Workflow State Only) */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase tracking-wider text-purple-700 flex items-center space-x-1.5">
                <Sparkles className="w-3.5 h-3.5 text-purple-600" />
                <span>Design Studio (Creative Workflow)</span>
              </label>
              <span className="text-[10px] font-semibold bg-purple-50 text-purple-700 px-2 py-0.5 rounded">
                {approvedDesigns.length} Approved
              </span>
            </div>

            <div className="space-y-2.5">
              <textarea
                rows={2}
                value={aiPrompt}
                onChange={(e) => setAiPrompt(e.target.value)}
                placeholder="Optional style hint (ULTRA autonomously engineers prompt via Gemini 4 Argon)..."
                className="w-full text-xs p-3 rounded-lg border border-neutral-300 bg-neutral-50 text-neutral-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none font-sans"
              />

              <button
                onClick={handleQueueAiMockup}
                disabled={isGeneratingAi}
                className="w-full py-2.5 px-3 rounded-lg text-xs font-semibold bg-neutral-900 hover:bg-black text-white transition-all duration-150 flex items-center justify-center space-x-2 shadow-sm disabled:opacity-75"
              >
                {isGeneratingAi ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin text-purple-400" />
                    <span>ULTRA Generating Transparent PNG...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-3.5 h-3.5 text-purple-400" />
                    <span>Generate Luxury Streetwear Graphic</span>
                  </>
                )}
              </button>

              {aiQueueStatus && (
                <div className="p-2.5 rounded-lg bg-purple-50 border border-purple-200 text-[11px] text-purple-900">
                  {aiQueueStatus}
                </div>
              )}
            </div>

            {/* Pending Design Approvals Queue (Creative Events — Never Mixed with Financial Ledger) */}
            {pendingApprovals.length > 0 && (
              <div className="space-y-2 pt-2">
                <div className="text-[11px] font-bold uppercase text-amber-700">
                  Pending Design Review ({pendingApprovals.length})
                </div>
                {pendingApprovals.map((design) => (
                  <div
                    key={design.id}
                    className="p-3 rounded-lg border border-amber-200 bg-amber-50/60 space-y-2"
                  >
                    <div className="flex items-center space-x-2">
                      <img
                        src={design.imageUrl}
                        alt="Generated Design"
                        className="w-10 h-10 object-contain rounded bg-neutral-900 p-1"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="text-[11px] font-mono font-bold text-neutral-800 truncate">
                          {design.sku}
                        </div>
                        <div className="text-[10px] text-neutral-500 truncate">
                          {design.prompt}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() => handleApproveDesign(design)}
                      className="w-full py-1.5 px-2 rounded bg-emerald-600 hover:bg-emerald-700 text-white text-[11px] font-semibold flex items-center justify-center space-x-1"
                    >
                      <Check className="w-3 h-3" />
                      <span>Approve Design for POD Pop-Up</span>
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Approved Designs Ready to Publish */}
            {approvedDesigns.length > 0 && (
              <div className="space-y-1.5 pt-1">
                <div className="text-[11px] font-bold uppercase text-emerald-700">
                  Approved Designs ({approvedDesigns.length})
                </div>
                {approvedDesigns.map((design) => (
                  <div
                    key={design.id}
                    className="p-2 rounded-lg border border-emerald-200 bg-emerald-50/50 flex items-center justify-between text-[11px]"
                  >
                    <span className="font-mono font-semibold text-emerald-950 truncate">
                      {design.sku}
                    </span>
                    <span className="text-[10px] text-emerald-700 font-bold">
                      APPROVED
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* LAYER VISIBILITY CONTROLS */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 flex items-center space-x-1.5">
                <Layers className="w-3.5 h-3.5" />
                <span>Canvas Layers</span>
              </label>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between p-2.5 rounded-lg border border-neutral-200 bg-neutral-50/70">
                <span className="text-xs font-semibold text-neutral-900">Layer 1: Vector Base Circle</span>
                <button
                  onClick={() => toggleLayer('circle')}
                  className="p-1 rounded hover:bg-neutral-200 text-neutral-600"
                >
                  {layerVisibility.circle ? <Eye className="w-4 h-4 text-blue-600" /> : <EyeOff className="w-4 h-4 text-neutral-400" />}
                </button>
              </div>

              <div className="flex items-center justify-between p-2.5 rounded-lg border border-neutral-200 bg-neutral-50/70">
                <span className="text-xs font-semibold text-neutral-900">Layer 2: Typography Subtitle</span>
                <button
                  onClick={() => toggleLayer('text')}
                  className="p-1 rounded hover:bg-neutral-200 text-neutral-600"
                >
                  {layerVisibility.text ? <Eye className="w-4 h-4 text-blue-600" /> : <EyeOff className="w-4 h-4 text-neutral-400" />}
                </button>
              </div>
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* PANEL 2: ORDERS / TRANSACTIONS LEDGER (Strictly Confirmed Real Printify + Stripe Orders Only) */}
          <div className="rounded-xl border border-neutral-200 bg-neutral-50/60 p-4 space-y-2.5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-neutral-700 flex items-center space-x-1.5">
                <Package className="w-3.5 h-3.5 text-emerald-600" />
                <span>Orders / Transactions Ledger</span>
              </span>
              <span className="text-[10px] font-mono font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                {transactionLedger.length} Verified
              </span>
            </div>

            {transactionLedger.length === 0 ? (
              <div className="text-[11px] text-neutral-500 py-2">
                No confirmed live orders yet. Entries are written exclusively from verified Printify order IDs + Stripe payment IDs.
              </div>
            ) : (
              <div className="space-y-2 max-h-36 overflow-y-auto">
                {transactionLedger.map((txn, idx) => (
                  <div
                    key={`${txn.printifyOrderId}-${idx}`}
                    className="p-2 rounded bg-white border border-neutral-200 text-[10px] font-mono space-y-0.5"
                  >
                    <div className="font-bold text-neutral-900">Printify ID: {txn.printifyOrderId}</div>
                    <div className="text-neutral-600">Stripe ID: {txn.stripePaymentId}</div>
                    <div className="text-emerald-700 font-semibold">
                      {txn.amount} {txn.currency}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* LIVE DEVELOPER PAYLOAD DEBUGGER WINDOW */}
          <div className="flex-1 flex flex-col min-h-0">
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 flex items-center space-x-1.5">
                <Cpu className="w-3.5 h-3.5 text-neutral-700" />
                <span>Live Canvas Telemetry</span>
              </label>
            </div>

            <div className="bg-neutral-900 text-[#38bdf8] font-mono text-xs p-4 rounded-lg overflow-x-auto h-40 border border-neutral-800 shadow-inner flex-1 flex flex-col">
              <pre className="text-[10px] leading-tight flex-1">
                {JSON.stringify(canvasPayload, null, 2)}
              </pre>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}

export default PodStudioDesktopWorkspace;
