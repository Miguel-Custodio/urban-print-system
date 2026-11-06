package com.urbanprint.backend.service;

import com.urbanprint.backend.model.*;
import com.urbanprint.backend.repository.MarkupTierRepository;
import com.urbanprint.backend.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CustomPricingService {

    public record Request(Long productId, BigDecimal width, BigDecimal height, Integer quantity,
                          List<Long> optionIds, Map<Long, Integer> optionQty) {}

    public record Line(String label, BigDecimal cost, String detail) {}

    public record Result(BigDecimal areaSqftPerPiece, BigDecimal perimeterFtPerPiece,
                         List<Line> lines, BigDecimal totalCost, BigDecimal markup,
                         BigDecimal sellPrice, BigDecimal pricePerPiece, List<String> warnings) {}

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    // Markup padrão (100%) quando nenhuma faixa de markup cobre o custo
    private static final BigDecimal DEFAULT_MULTIPLIER = new BigDecimal("2.00");

    private final ProductRepository productRepository;
    private final MarkupTierRepository markupRepository;
    private final CutPlanService cutPlanService;

    public CustomPricingService(ProductRepository p, MarkupTierRepository m, CutPlanService c) {
        this.productRepository = p;
        this.markupRepository = m;
        this.cutPlanService = c;
    }

    private static BigDecimal n(BigDecimal v) { return v == null ? ZERO : v; }
    private static BigDecimal r2(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    public Result calculate(Request req) {
        Product p = productRepository.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        int qty = req.quantity() == null || req.quantity() < 1 ? 1 : req.quantity();
        BigDecimal w = req.width(), h = req.height();
        if (w == null || h == null || w.signum() <= 0 || h.signum() <= 0)
            throw new IllegalArgumentException("Width and height are required");

        List<String> warnings = new ArrayList<>();
        List<Line> lines = new ArrayList<>();
        BigDecimal q = BigDecimal.valueOf(qty);

        BigDecimal area = w.multiply(h).divide(BigDecimal.valueOf(144), 6, RoundingMode.HALF_UP);
        BigDecimal perimeterFt = w.add(h).multiply(BigDecimal.valueOf(2))
                .divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal totalArea = area.multiply(q);

        // 1) Printing (per sq ft)
        BigDecimal print = totalArea.multiply(n(p.getPrintCostPerSqft()));
        if (p.getPrintCostPerSqft() == null) warnings.add("Print cost per sq ft not set");
        lines.add(new Line("Printing", r2(print),
                totalArea.setScale(2, RoundingMode.HALF_UP) + " sq ft x " + n(p.getPrintCostPerSqft())));

        // 2) Stock — cobrado por sq ft (custo da chapa / área da chapa x área usada)
        BigDecimal stock;
        String stockDetail;
        BigDecimal stockRate = null;
        if (p.getStockSheetWidth() != null && p.getStockSheetHeight() != null
                && p.getStockSheetCost() != null
                && p.getStockSheetWidth().signum() > 0 && p.getStockSheetHeight().signum() > 0) {
            BigDecimal sheetSqft = p.getStockSheetWidth().multiply(p.getStockSheetHeight())
                    .divide(BigDecimal.valueOf(144), 6, RoundingMode.HALF_UP);
            stockRate = p.getStockSheetCost().divide(sheetSqft, 6, RoundingMode.HALF_UP);
            int perSheet = cutPlanService.plan(p.getStockSheetWidth().doubleValue(),
                    p.getStockSheetHeight().doubleValue(), w.doubleValue(), h.doubleValue(),
                    1, 0, 0, true).perSheet();
            if (perSheet == 0) {
                warnings.add("Piece does not fit in the sheet; check sheet size / needs seam");
            }
            stock = totalArea.multiply(stockRate);
            stockDetail = totalArea.setScale(2, RoundingMode.HALF_UP) + " sq ft x "
                    + stockRate.setScale(4, RoundingMode.HALF_UP)
                    + " (sheet " + p.getStockSheetWidth() + "x" + p.getStockSheetHeight()
                    + ", up to " + perSheet + " per sheet)";
        } else {
            stockRate = n(p.getStockCostPerSqft());
            stock = totalArea.multiply(stockRate);
            stockDetail = totalArea.setScale(2, RoundingMode.HALF_UP) + " sq ft x " + stockRate;
            if (p.getStockCostPerSqft() == null) warnings.add("Stock cost not set");
        }
        lines.add(new Line("Stock", r2(stock), stockDetail));

        // 3) Cutting
        BigDecimal cut = perimeterFt.multiply(q).multiply(n(p.getCutCostPerLinearFt()));
        if (cut.compareTo(n(p.getCutMinimum())) < 0) cut = n(p.getCutMinimum());
        lines.add(new Line("Cutting", r2(cut),
                perimeterFt.multiply(q).setScale(2, RoundingMode.HALF_UP) + " linear ft"));

        // 4) Selected options (grommets, etc.)
        BigDecimal optionsTotal = ZERO;
        Set<Long> chosen = req.optionIds() == null ? Set.of() : new HashSet<>(req.optionIds());
        for (ProductOption o : p.getOptions()) {
            if (!o.active || !chosen.contains(o.id)) continue;
            BigDecimal c = n(o.cost), amount; String d;
            switch (o.costType == null ? "FLAT" : o.costType) {
                case "PER_UNIT" -> {
                    int perPiece;
                    Integer override = req.optionQty() == null ? null : req.optionQty().get(o.id);
                    if (override != null) {
                        perPiece = override;
                    } else if (o.spacingInches != null && o.spacingInches > 0) {
                        perPiece = perimeterFt.multiply(BigDecimal.valueOf(12))
                                .divide(BigDecimal.valueOf(o.spacingInches), 0, RoundingMode.CEILING).intValue();
                        if (o.minQty != null) perPiece = Math.max(perPiece, o.minQty);
                    } else {
                        perPiece = o.minQty == null ? 1 : o.minQty;
                    }
                    int units = perPiece * qty;
                    amount = c.multiply(BigDecimal.valueOf(units));
                    d = units + " units";
                }
                case "PER_LINEAR_FT" -> { amount = c.multiply(perimeterFt).multiply(q); d = "perimeter"; }
                case "PER_SQFT" -> { amount = c.multiply(totalArea); d = "area"; }
                default -> { amount = c; d = "flat"; }
            }
            optionsTotal = optionsTotal.add(amount);
            lines.add(new Line(o.name, r2(amount), d));
        }

        BigDecimal totalCost = print.add(stock).add(cut).add(optionsTotal);
        if (totalCost.signum() == 0) {
            warnings.add("Total cost is $0.00 - fill in the product cost fields (print, stock, cut)");
        }

        // 5) Markup by cost tier (padrão 100% = x2.00 se nenhuma faixa cobrir)
        BigDecimal multiplier = DEFAULT_MULTIPLIER;
        boolean found = false;
        for (MarkupTier t : markupRepository.findAllByOrderByMinCostAsc()) {
            boolean aboveMin = totalCost.compareTo(n(t.minCost)) >= 0;
            boolean belowMax = t.maxCost == null || totalCost.compareTo(t.maxCost) < 0;
            if (aboveMin && belowMax) { multiplier = t.multiplier; found = true; break; }
        }
        if (!found) warnings.add("No markup tier found for this cost; using default 100% (x2.00)");

        BigDecimal sell = totalCost.multiply(multiplier);
        if (p.getMinimumOrderFee() != null && sell.compareTo(p.getMinimumOrderFee()) < 0) {
            sell = p.getMinimumOrderFee();
            warnings.add("Minimum order fee applied");
        }

        return new Result(area.setScale(2, RoundingMode.HALF_UP), perimeterFt.setScale(2, RoundingMode.HALF_UP),
                lines, r2(totalCost), multiplier, r2(sell),
                r2(sell.divide(q, 4, RoundingMode.HALF_UP)), warnings);
    }
}