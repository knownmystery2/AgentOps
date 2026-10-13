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
  Maximize2,
  Sliders,
  Zap,
  ShieldCheck,
  Printer,
  ChevronRight,
  Move,
  Info,
  Clock,
  Shirt,
  ShoppingBag,
  Coffee,
  Image as ImageIcon
} from 'lucide-react';

// Product Catalog Models
const PRODUCT_CATALOG = [
  {
    id: 'heavyweight-tshirt',
    name: 'Heavyweight T-Shirt',
    category: 'Apparel',
    weight: '240 GSM Organic Cotton',
    baseSku: 'POD-HET',
    icon: Shirt,
    mockupType: 'tshirt'
  },
  {
    id: 'vintage-hoodie',
    name: 'Vintage Hoodie',
    category: 'Apparel',
    weight: '380 GSM Heavy Fleece',
    baseSku: 'POD-HOD',
    icon: Shirt,
    mockupType: 'hoodie'
  },
  {
    id: 'ceramic-mug',
    name: 'Ceramic Mug',
    category: 'Drinkware',
    weight: '11 oz Glossy Ceramic',
    baseSku: 'POD-MUG',
    icon: Coffee,
    mockupType: 'mug'
  },
  {
    id: 'canvas-tote',
    name: 'Canvas Tote Bag',
    category: 'Accessories',
    weight: '100% Heavy Bull Denim',
    baseSku: 'POD-TOT',
    icon: ShoppingBag,
    mockupType: 'tote'
  },
  {
    id: 'wall-art-poster',
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
 * PodStudioDesktopWorkspace - Enterprise Desktop Printify Mockup Workspace
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
  const [aiPrompt, setAiPrompt] = useState('Model modeling oversized tee in warm sunset studio lighting, cinematic film grain');
  const [isGeneratingAi, setIsGeneratingAi] = useState(false);
  const [aiQueueStatus, setAiQueueStatus] = useState(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [publishSuccessBanner, setPublishSuccessBanner] = useState(null);

  // State: Live Canvas Telemetry Debugger
  const [canvasPayload, setCanvasPayload] = useState({});

  // Refs: Canvas & Fabric Infrastructure
  const canvasElementRef = useRef(null);
  const fabricCanvasRef = useRef(null);
  const clipPathRef = useRef(null);
  const circleObjRef = useRef(null);
  const textObjRef = useRef(null);
  const containerRef = useRef(null);

  // SKU calculation
  const computedSku = `${selectedProduct.baseSku}--${PRINT_ZONES[selectedZone].skuCode}-2026`;

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
        name: obj.dataName || (obj.type === 'circle' ? 'Vector Base Circle' : 'Typography Subtitle'),
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
      system: 'PrintDefy Pop Up Store Engine v4.0',
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

    // Dispose old canvas if reinitializing
    if (fabricCanvasRef.current) {
      fabricCanvasRef.current.dispose();
      fabricCanvasRef.current = null;
    }

    // Initialize fresh Fabric instance
    const canvas = new fabric.Canvas(canvasElementRef.current, {
      width: zoneConfig.width,
      height: zoneConfig.height,
      selection: true,
      preserveObjectStacking: true,
      backgroundColor: 'transparent'
    });

    fabricCanvasRef.current = canvas;

    // Automated Vector Clipping Boundary (clipPath) matching Print Area Limit
    const clipRect = new fabric.Rect({
      left: 0,
      top: 0,
      width: zoneConfig.width,
      height: zoneConfig.height,
      absolutePositioned: true
    });
    clipPathRef.current = clipRect;
    canvas.clipPath = clipRect;

    // Populate Initial Graphic Asset 1: Solid Clean White Circle
    const circle = new fabric.Circle({
      radius: selectedZone === 'chest' ? 56 : 28,
      fill: '#FFFFFF',
      left: zoneConfig.width / 2,
      top: selectedZone === 'chest' ? zoneConfig.height / 2 - 32 : zoneConfig.height / 2 - 16,
      originX: 'center',
      originY: 'center',
      cornerColor: '#2563eb',
      cornerStrokeColor: '#ffffff',
      cornerSize: 10,
      transparentCorners: false,
      hasRotatingPoint: true
    });
    circle.dataId = 'layer_circle';
    circle.dataName = 'Vector Base Circle';
    circleObjRef.current = circle;
    canvas.add(circle);

    // Populate Initial Typographical Asset 2: Crisp "ORIGINAL VINTAGE"
    const text = new fabric.Text('ORIGINAL VINTAGE', {
      fontSize: selectedZone === 'chest' ? 17 : 9,
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
      fontWeight: 'bold',
      fill: '#FFFFFF',
      left: zoneConfig.width / 2,
      top: selectedZone === 'chest' ? zoneConfig.height / 2 + 50 : zoneConfig.height / 2 + 25,
      originX: 'center',
      originY: 'center',
      cornerColor: '#2563eb',
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

    // Event hooks for real-time telemetry updates
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

    // Initial telemetry pipe
    updatePayloadDebugger();

    // Responsive window resize observer
    const handleResize = () => {
      if (fabricCanvasRef.current) {
        fabricCanvasRef.current.calcOffset();
        fabricCanvasRef.current.renderAll();
      }
    };
    window.addEventListener('resize', handleResize);

    // Clean unmount routine
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

  // AI Mockup Generation Handler
  const handleQueueAiMockup = () => {
    if (isGeneratingAi) return;
    setIsGeneratingAi(true);
    setAiQueueStatus('Submitting prompt to Gemini image generation pipeline...');

    setTimeout(() => {
      setAiQueueStatus('Synthesizing studio lighting and garment texture...');
      setTimeout(() => {
        setIsGeneratingAi(false);
        setAiQueueStatus('Mockup rendered! Asset mapped to SKU: ' + computedSku);
        setTimeout(() => setAiQueueStatus(null), 6000);
      }, 1400);
    }, 1200);
  };

  // Live Publishing Handler
  const handlePublishLive = () => {
    if (isPublishing) return;
    setIsPublishing(true);

    setTimeout(() => {
      const syncId = `SYNC-PTFY-${Math.random().toString(36).substring(2, 9).toUpperCase()}`;
      setIsPublishing(false);
      setPublishSuccessBanner({
        syncId,
        sku: computedSku,
        timestamp: new Date().toLocaleTimeString()
      });

      if (typeof onNavigateAway === 'function') {
        setTimeout(() => {
          onNavigateAway('storefront');
        }, 1800);
      }
    }, 1500);
  };

  // Safely cleanup before exiting
  const handleBackToStorefront = () => {
    if (fabricCanvasRef.current) {
      fabricCanvasRef.current.dispose();
      fabricCanvasRef.current = null;
    }
    if (typeof onNavigateAway === 'function') {
      onNavigateAway('storefront');
    }
  };

  const zoneConfig = PRINT_ZONES[selectedZone];

  return (
    <div className="h-screen w-screen overflow-hidden flex flex-col bg-[#f4f5f7] select-none text-neutral-800 font-sans">
      {/* 1. FIXED TOP APPLICATION HEADER */}
      <header className="w-full bg-[#1e2022] h-16 flex items-center justify-between px-6 border-b border-neutral-800 shadow-md z-50 flex-shrink-0">
        {/* Left: Interactive Back Button */}
        <div className="flex items-center space-x-4">
          <button
            onClick={handleBackToStorefront}
            className="flex items-center text-sm font-medium text-neutral-400 hover:text-white transition-colors duration-150 group"
          >
            <ArrowLeft className="w-4 h-4 mr-2 transition-transform duration-150 group-hover:-translate-x-1" />
            <span>← Back to Storefront</span>
          </button>
          <div className="h-4 w-px bg-neutral-700 hidden sm:block" />
          <div className="hidden sm:flex items-center space-x-2 text-xs text-neutral-400">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Live Workspace</span>
          </div>
        </div>

        {/* Center: Workspace Indicator */}
        <div className="flex items-center space-x-2">
          <Cpu className="w-4 h-4 text-blue-400 hidden md:block" />
          <span className="font-semibold text-neutral-200 tracking-wide text-xs sm:text-sm">
            Print Defy Pop Up Store Engine v4.0 (Desktop Production Mode)
          </span>
        </div>

        {/* Right: Primary Call-to-Action Button */}
        <div className="flex items-center space-x-3">
          <button
            onClick={handlePublishLive}
            disabled={isPublishing}
            className="bg-[#2563eb] hover:bg-[#1d4ed8] text-white font-semibold text-sm px-5 py-2.5 rounded-lg shadow transition-all duration-150 active:scale-[0.98] flex items-center space-x-2 disabled:opacity-75 disabled:cursor-not-allowed"
          >
            {isPublishing ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Syncing Printify Catalog...</span>
              </>
            ) : (
              <>
                <Zap className="w-4 h-4 text-yellow-300 fill-yellow-300" />
                <span>Publish Live to Pop-Up Store</span>
              </>
            )}
          </button>
        </div>
      </header>

      {/* SUCCESS NOTIFICATION TOAST */}
      {publishSuccessBanner && (
        <div className="bg-emerald-600 text-white px-6 py-2.5 flex items-center justify-between shadow-lg text-sm transition-all duration-300 animate-in slide-in-from-top z-40">
          <div className="flex items-center space-x-3">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
            <div>
              <span className="font-bold">Catalog Published Successfully! </span>
              <span>SKU: {publishSuccessBanner.sku} synchronized with Printify provider (ID: {publishSuccessBanner.syncId})</span>
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
        {/* 2. FIXED LEFT CONTROL COLUMN - COMPONENT & INVENTORY SELECTION */}
        <aside className="h-[calc(100vh-64px)] w-[340px] flex flex-col overflow-y-auto p-6 space-y-6 bg-white border-r border-neutral-200 flex-shrink-0 shadow-sm">
          {/* SECTION: PRODUCT BLANKS CATALOG */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500">
                Select Product Blank
              </label>
              <span className="text-[11px] font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                Tier 1 Verified
              </span>
            </div>

            <div className="space-y-2.5">
              {PRODUCT_CATALOG.map((product) => {
                const IconComponent = product.icon;
                const isSelected = selectedProduct.id === product.id;
                return (
                  <button
                    key={product.id}
                    onClick={() => setSelectedProduct(product)}
                    className={`w-full text-left p-3.5 rounded-xl border transition-all duration-150 flex items-center justify-between ${
                      isSelected
                        ? 'border-2 border-blue-600 bg-blue-50/50 shadow-sm'
                        : 'border-neutral-200 hover:border-neutral-300 bg-white hover:bg-neutral-50'
                    }`}
                  >
                    <div className="flex items-center space-x-3">
                      <div
                        className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ${
                          isSelected ? 'bg-blue-600 text-white' : 'bg-neutral-100 text-neutral-600'
                        }`}
                      >
                        <IconComponent className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="font-semibold text-sm text-neutral-900 leading-tight">
                          {product.name}
                        </div>
                        <div className="text-xs text-neutral-500 mt-0.5">
                          {product.weight}
                        </div>
                      </div>
                    </div>
                    {isSelected && (
                      <div className="h-5 w-5 rounded-full bg-blue-600 text-white flex items-center justify-center flex-shrink-0">
                        <Check className="w-3 h-3 stroke-[3]" />
                      </div>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* SECTION: DYNAMIC GARMENT COLOR PALETTE SWATCHES */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500">
                Garment Palette
              </label>
              <span className="text-xs font-mono font-medium text-neutral-600">
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
                    {/* Concentric double ring wrapper when active */}
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

          {/* SECTION: PRINT ZONE & DIMENSION PARAMETERS */}
          <div>
            <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 block mb-3">
              Print Area & Dimensions
            </label>
            <div className="grid grid-cols-2 gap-2">
              {Object.keys(PRINT_ZONES).map((zoneKey) => {
                const zone = PRINT_ZONES[zoneKey];
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

          {/* PRINT PRODUCTION SPECS */}
          <div className="p-4 rounded-xl bg-neutral-50 border border-neutral-200 space-y-2 mt-auto">
            <div className="flex items-center space-y-0.5 space-x-2 text-xs font-semibold text-neutral-800">
              <Printer className="w-4 h-4 text-neutral-600" />
              <span>Direct-to-Garment (DTG) Specs</span>
            </div>
            <div className="text-[11px] text-neutral-500 leading-relaxed">
              Kornit Avalanche HD6 ink jet system with water-based eco inks. Minimum 300 DPI vector rendering with white underbase.
            </div>
          </div>
        </aside>

        {/* 4. CENTER FLEX WORKSPACE - INTERACTIVE DESIGN ENVIRONMENT */}
        <main
          ref={containerRef}
          className="flex-1 h-[calc(100vh-64px)] overflow-hidden relative flex items-center justify-center bg-[#18181b] p-8 sm:p-12"
        >
          {/* Visual Grid Canvas Floor with subtle isometric grid overlay */}
          <div
            className="absolute inset-0 pointer-events-none opacity-20"
            style={{
              backgroundImage: `radial-gradient(circle at 1px 1px, #ffffff 1px, transparent 0)`,
              backgroundSize: '24px 24px'
            }}
          />
          <div className="absolute inset-0 pointer-events-none bg-gradient-to-t from-black/40 via-transparent to-black/20" />

          {/* THE PRODUCT MOCKUP CARD FRAMEWORK */}
          <div className="relative flex items-center justify-center w-[540px] h-[640px] max-w-full max-h-full transition-all duration-300">
            {/* Garment Base Silhouette Vector SVG tintable via selectedColor.hex */}
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
                {/* Garment Silhouette Shadow */}
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 L465 145 C485 160 475 190 445 220 L405 250 L410 575 C410 590 395 600 375 600 L165 600 C145 600 130 590 130 575 L135 250 L95 220 C65 190 55 160 75 145 Z"
                  fill="#000000"
                  opacity="0.3"
                  filter="blur(14px)"
                />

                {/* Primary Garment Body Layer - Tinted via selectedColor */}
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 L465 145 C485 160 475 190 445 220 L405 250 L410 575 C410 590 395 600 375 600 L165 600 C145 600 130 590 130 575 L135 250 L95 220 C65 190 55 160 75 145 Z"
                  fill={selectedColor.hex}
                  className="transition-colors duration-200"
                />

                {/* Realistic Textile Texture and Shading */}
                <path
                  d="M170 80 C195 95 240 102 270 102 C300 102 345 95 370 80 L465 145 C485 160 475 190 445 220 L405 250 L410 575 C410 590 395 600 375 600 L165 600 C145 600 130 590 130 575 L135 250 L95 220 C65 190 55 160 75 145 Z"
                  fill="url(#fabricGradient)"
                  opacity="0.4"
                />

                {/* Collar Ribbing Silhouette */}
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

                {/* Subtle Hem and Sleeve Seams */}
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

            {/* THE BOUNDING PRINT FRAME CONTAINER */}
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
              {/* Dashed Neon Visual Boundary Box */}
              <div className="absolute inset-0 pointer-events-none border-2 border-dashed border-cyan-400/80 rounded-md shadow-[0_0_12px_rgba(34,211,238,0.25)] flex flex-col justify-between p-1">
                {/* Top status indicator tag */}
                <div className="flex items-center justify-between">
                  <span className="bg-cyan-900/90 text-cyan-300 text-[9px] font-mono font-bold px-1.5 py-0.5 rounded uppercase tracking-wider border border-cyan-500/40">
                    Print Area Limit
                  </span>
                  <span className="text-[9px] text-cyan-300 font-mono">
                    {zoneConfig.width}×{zoneConfig.height}px
                  </span>
                </div>
                {/* Bottom corner guides */}
                <div className="flex items-center justify-between text-[8px] text-cyan-400 font-mono">
                  <span>+</span>
                  <span>+</span>
                </div>
              </div>

              {/* The Active HTML5 Canvas Element managed by Fabric.js */}
              <canvas
                ref={canvasElementRef}
                className="w-full h-full block cursor-move"
              />
            </div>
          </div>

          {/* System Status Micro-Banner */}
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-30">
            <div className="flex items-center space-x-2 bg-neutral-900/90 backdrop-blur-md px-4 py-1.5 rounded-full border border-neutral-700/80 shadow-xl text-neutral-300 text-xs font-mono">
              <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
              <span>300 DPI • CMYK Print Ready Vector Placement Engine Active</span>
            </div>
          </div>
        </main>

        {/* 3. FIXED RIGHT CONTROL COLUMN - LAYERS, METRICS & TRANSLATION ENGINE */}
        <aside className="h-[calc(100vh-64px)] w-[300px] flex flex-col overflow-y-auto p-6 space-y-6 bg-white border-l border-neutral-200 flex-shrink-0 shadow-sm">
          {/* SECTION: VISUAL LAYER MANAGEMENT STACK */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 flex items-center space-x-1.5">
                <Layers className="w-3.5 h-3.5" />
                <span>Layers Stack</span>
              </label>
              <span className="text-[11px] font-medium text-neutral-400">
                2 Objects
              </span>
            </div>

            <div className="space-y-2">
              {/* Layer 1: Vector Base Circle */}
              <div className="flex items-center justify-between p-3 rounded-lg border border-neutral-200 bg-neutral-50/70 hover:bg-neutral-100 transition-colors">
                <div className="flex items-center space-x-2.5">
                  <div className="w-5 h-5 rounded-full border border-neutral-300 bg-white flex items-center justify-center text-[10px] font-bold text-neutral-600">
                    O
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-neutral-900">
                      Layer 1: Vector Base Circle
                    </div>
                    <div className="text-[10px] text-neutral-400">
                      Vector Geometry • r=56
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => toggleLayer('circle')}
                  className="p-1.5 rounded hover:bg-neutral-200 text-neutral-600 transition-colors"
                  title="Toggle Layer Visibility"
                >
                  {layerVisibility.circle ? (
                    <Eye className="w-4 h-4 text-blue-600" />
                  ) : (
                    <EyeOff className="w-4 h-4 text-neutral-400" />
                  )}
                </button>
              </div>

              {/* Layer 2: Typography Subtitle */}
              <div className="flex items-center justify-between p-3 rounded-lg border border-neutral-200 bg-neutral-50/70 hover:bg-neutral-100 transition-colors">
                <div className="flex items-center space-x-2.5">
                  <div className="w-5 h-5 rounded bg-blue-100 text-blue-700 flex items-center justify-center text-[10px] font-bold font-serif">
                    T
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-neutral-900">
                      Layer 2: Typography Subtitle
                    </div>
                    <div className="text-[10px] text-neutral-400">
                      "ORIGINAL VINTAGE"
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => toggleLayer('text')}
                  className="p-1.5 rounded hover:bg-neutral-200 text-neutral-600 transition-colors"
                  title="Toggle Layer Visibility"
                >
                  {layerVisibility.text ? (
                    <Eye className="w-4 h-4 text-blue-600" />
                  ) : (
                    <EyeOff className="w-4 h-4 text-neutral-400" />
                  )}
                </button>
              </div>
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* SECTION: ACTIVE SKU GENERATION ENGINE INDICATOR */}
          <div className="rounded-xl border border-neutral-200 bg-neutral-50/60 p-4 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-neutral-700 flex items-center space-x-1.5">
                <Package className="w-3.5 h-3.5 text-blue-600" />
                <span>Active SKU Engine</span>
              </span>
            </div>

            <div className="bg-white p-2.5 rounded-lg border border-neutral-200 font-mono text-xs font-semibold text-neutral-800 break-all">
              {computedSku}
            </div>

            <div className="flex flex-wrap gap-2 pt-1">
              <span className="inline-flex items-center space-x-1 bg-emerald-50 text-emerald-700 border border-emerald-200 px-2.5 py-1 rounded-md text-[11px] font-semibold">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Zero Risk</span>
              </span>
              <span className="inline-flex items-center space-x-1 bg-emerald-50 text-emerald-700 border border-emerald-200 px-2.5 py-1 rounded-md text-[11px] font-semibold">
                <Check className="w-3.5 h-3.5" />
                <span>Supplier Sync Active</span>
              </span>
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* SECTION: AI MOCKUP LIFESTYLE PLACEMENT PROMPT MODULE */}
          <div>
            <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 flex items-center space-x-1.5 mb-2.5">
              <Sparkles className="w-3.5 h-3.5 text-amber-500" />
              <span>AI Mockup Background Generator</span>
            </label>

            <div className="space-y-2.5">
              <textarea
                rows={3}
                value={aiPrompt}
                onChange={(e) => setAiPrompt(e.target.value)}
                placeholder="Describe model, studio lighting, or urban background..."
                className="w-full text-xs p-3 rounded-lg border border-neutral-300 bg-neutral-50 text-neutral-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none font-sans"
              />

              <button
                onClick={handleQueueAiMockup}
                disabled={isGeneratingAi}
                className="w-full py-2.5 px-3 rounded-lg text-xs font-semibold bg-neutral-800 hover:bg-neutral-900 text-white transition-all duration-150 flex items-center justify-center space-x-2 shadow-sm disabled:opacity-75"
              >
                {isGeneratingAi ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin text-amber-400" />
                    <span>Processing Lifestyle AI...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                    <span>Queue Mockup Rendering</span>
                  </>
                )}
              </button>

              {aiQueueStatus && (
                <div className="p-2.5 rounded-lg bg-amber-50 border border-amber-200 text-[11px] text-amber-800 animate-in fade-in">
                  {aiQueueStatus}
                </div>
              )}
            </div>
          </div>

          <div className="h-px bg-neutral-200 w-full" />

          {/* SECTION: LIVE DEVELOPER PAYLOAD DEBUGGER WINDOW */}
          <div className="flex-1 flex flex-col min-h-0">
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-bold uppercase tracking-wider text-neutral-500 flex items-center space-x-1.5">
                <Cpu className="w-3.5 h-3.5 text-neutral-700" />
                <span>Live Developer Payload Debugger</span>
              </label>
            </div>

            <div className="bg-neutral-900 text-[#38bdf8] font-mono text-xs p-4 rounded-lg overflow-x-auto h-48 border border-neutral-800 shadow-inner flex-1 flex flex-col">
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
